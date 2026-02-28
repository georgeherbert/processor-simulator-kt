#include <stdlib.h>
#include <stdio.h>
#include "commit.h"
#include "rob.h"
#include "decoded_inst.h"
#include "res_stations.h"
#include "memory_buffers.h"
#include "alu.h"
#include "branch.h"
#include "inst_queue.h"
#include "reg_file.h"
#include "control.h"
#include "address.h"
#include "memory.h"

struct commit_unit *commit_init(
    struct rob *rob,
    struct main_memory *mm,
    struct reg_file *reg_file,
    struct res_stations *alu_rs,
    struct res_stations *branch_rs,
    struct memory_buffers *mb,
    struct alu_unit **alus,
    struct branch_unit **branch_units,
    struct address_unit **address_units,
    struct memory_unit **memory_units,
    struct inst_queue *iq,
    struct reg *reg_insts,
    struct reg *pc_src,
    struct reg *reg_pc_target,
    struct btb *btb,
    bool *jump_zero)
{
    struct commit_unit *commit_unit = malloc(sizeof(struct commit_unit));
    if (commit_unit == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for commit unit");
        exit(EXIT_FAILURE);
    }

    commit_unit->rob = rob;
    commit_unit->mm = mm;
    commit_unit->reg_file = reg_file;
    commit_unit->alu_rs = alu_rs;
    commit_unit->branch_rs = branch_rs;
    commit_unit->mb = mb;
    commit_unit->alus = alus;
    commit_unit->branch_units = branch_units;
    commit_unit->address_units = address_units;
    commit_unit->memory_units = memory_units;
    commit_unit->iq = iq;
    commit_unit->reg_insts = reg_insts;
    commit_unit->pc_src = pc_src;
    commit_unit->reg_pc_target = reg_pc_target;
    commit_unit->btb = btb;
    commit_unit->jump_zero = jump_zero;

    return commit_unit;
}

void commit_clear(struct commit_unit *commit_unit)
{
    /*
        Incorrect branch prediction, we need to clear:
        - The ROB
        - ROB IDs in the register file
        - Reservation stations
        - ALU unit
        - Memory buffers
        - Instruction queue
        - Instruction register
    */
    rob_clear(commit_unit->rob);
    reg_file_clear(commit_unit->reg_file);
    res_stations_clear(commit_unit->alu_rs);
    res_stations_clear(commit_unit->branch_rs);
    memory_buffers_clear(commit_unit->mb);
    for (uint8_t i = 0; i < NUM_ALU_UNITS; i++)
    {
        alu_clear(commit_unit->alus[i]);
    }
    for (uint8_t i = 0; i < NUM_BRANCH_UNITS; i++)
    {
        branch_clear(commit_unit->branch_units[i]);
    }
    for (uint8_t i = 0; i < NUM_ADDRESS_UNITS; i++)
    {
        address_clear(commit_unit->address_units[i]);
    }
    for (uint8_t i = 0; i < NUM_MEMORY_UNITS; i++)
    {
        memory_clear(commit_unit->memory_units[i]);
    }
    inst_queue_clear(commit_unit->iq);
    for (uint8_t i = 0; i < ISSUE_WIDTH; i++)
    {
        reg_write(&commit_unit->reg_insts[i], 0x0);
    }
}

void update_btb(struct btb *btb, uint32_t inst_pc, uint32_t npc_actual)
{
    if (npc_actual != inst_pc + 4)
    {
        btb_taken(btb, inst_pc, npc_actual);
    }
    else
    {
        btb_not_taken(btb, inst_pc);
    }
}

bool single_commit(struct commit_unit *commit_unit, uint32_t *num_committed, uint32_t *num_branches, uint32_t *num_mispredicted, uint32_t commit_num)
{
    bool mispredict = false;
    if (rob_ready(commit_unit->rob, commit_num))
    {
        struct rob_entry entry = rob_dequeue(commit_unit->rob, commit_num);
        switch (entry.op_type)
        {
        case JUMP:
            update_btb(commit_unit->btb, entry.inst_pc, entry.npc_actual);
            reg_file_reg_commit(commit_unit->reg_file, entry.dest, entry.value, entry.rob_id);
            #ifdef VERBOSE
                printf("\tRF[%d] = %d\n", entry.dest, entry.value);
            #endif
            (*num_branches)++;
            if (entry.npc_actual != entry.npc_pred) // Misprediction
            {
                (*num_mispredicted)++;
                commit_clear(commit_unit);
                reg_write(commit_unit->reg_pc_target, entry.npc_actual);
                reg_write(commit_unit->pc_src, PC_SRC_MISPREDICT);
                mispredict = true;
            }
            *commit_unit->jump_zero = (entry.npc_actual == 0x0); // End of program
            // printf("\tJump %d %d\n", entry.dest, entry.value);
            break;
        case BRANCH:
            update_btb(commit_unit->btb, entry.inst_pc, entry.npc_actual);
            (*num_branches)++;
            if (entry.npc_actual != entry.npc_pred) // Misprediction
            {
                (*num_mispredicted)++;
                commit_clear(commit_unit);
                reg_write(commit_unit->reg_pc_target, entry.npc_actual);
                reg_write(commit_unit->pc_src, PC_SRC_MISPREDICT);
                mispredict = true;
            }
            // printf("\tBranch %d %d\n", entry.dest, entry.value);
            break;
        case LOAD:
        case AL:
            reg_file_reg_commit(commit_unit->reg_file, entry.dest, entry.value, entry.rob_id);
            #ifdef VERBOSE
                printf("\tRF[%d] = %d\n", entry.dest, entry.value);
            #endif
            // printf("\tAL/Load %d %d\n", entry.dest, entry.value);
            break;
        case STORE_WORD:
            main_memory_store_word(commit_unit->mm, entry.dest, entry.value);
            #ifdef VERBOSE
                printf("\tMM[%d] = %d\n", entry.dest, entry.value);
            #endif
            // printf("\tSW %d %d\n", entry.dest, entry.value);
            break;
        case STORE_HALF:
            main_memory_store_half(commit_unit->mm, entry.dest, entry.value);
            #ifdef VERBOSE
                printf("\tMM[%d] = %d\n", entry.dest, entry.value);
            #endif
            // printf("\tCSH %d %d\n", entry.dest, entry.value);
            break;
        case STORE_BYTE:
            main_memory_store_byte(commit_unit->mm, entry.dest, entry.value);
            #ifdef VERBOSE
                printf("\tMM[%d] = %d\n", entry.dest, entry.value);
            #endif
            // printf("\tSB %d %d\n", entry.dest, entry.value);
            break;
        default:
            fprintf(stderr, "Error: Invalid op type in commit step\n");
            exit(EXIT_FAILURE);
        }
        (*num_committed)++;
    }
    return mispredict;
}

void commit_step(struct commit_unit *commit_unit, uint32_t *num_committed, uint32_t *num_branches, uint32_t *num_mispredicted)
{
    for (uint32_t i = 0; i < COMMIT_WIDTH; i++)
    {
        bool mispredict = single_commit(commit_unit, num_committed, num_branches, num_mispredicted, i);
        if (mispredict)
        {
            break;
        }
    }
}

void commit_destroy(struct commit_unit *commit_unit)
{
    free(commit_unit);
}
