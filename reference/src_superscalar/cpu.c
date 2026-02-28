#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <inttypes.h>
#include "cpu.h"
#include "main_memory.h"
#include "reg_file.h"
#include "fetch.h"
#include "decode.h"
#include "inst_queue.h"
#include "issue.h"
#include "res_stations.h"
#include "reg_file.h"
#include "alu.h"
#include "branch.h"
#include "memory.h"
#include "control.h"
#include "reg.h"
#include "memory_buffers.h"
#include "address.h"
#include "rob.h"
#include "commit.h"
#include "btb.h"
#include "config.h"

#define NUM_WORDS_OUTPUT 2048

struct cpu *cpu_init(char *file_name)
{
    struct cpu *cpu = malloc(sizeof(struct cpu));
    if (cpu == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for cpu\n");
        exit(EXIT_FAILURE);
    }

    cpu->cdb = cdb_init(NUM_ALU_UNITS + NUM_BRANCH_UNITS + NUM_MEMORY_UNITS); // One entry for each functional unit
    cpu->reg_file = reg_file_init(cpu->cdb);

    cpu->pc_src.val_current = PC_SRC_NORMAL;
    cpu->jump_zero = false;

    cpu->mm = main_memory_init(
        file_name);
    cpu->inst_queue = inst_queue_init();
    cpu->rob = rob_init(
        cpu->cdb);
    cpu->btb = btb_init();
    cpu->fetch_unit = fetch_init(
        cpu->mm,
        &cpu->pc_src,
        cpu->inst_queue,
        &cpu->reg_pc_target,
        cpu->reg_insts,
        cpu->reg_inst_pcs,
        cpu->reg_npc_preds,
        cpu->btb);
    cpu->decode_unit = decode_init(
        cpu->reg_insts,
        cpu->reg_inst_pcs,
        cpu->inst_queue,
        cpu->reg_npc_preds);
    cpu->alu_res_stations = res_stations_init(
        NUM_ALU_RES_STATIONS,
        cpu->reg_file,
        cpu->cdb);
    cpu->branch_res_stations = res_stations_init(
        NUM_BRANCH_RES_STATIONS,
        cpu->reg_file,
        cpu->cdb);
    cpu->memory_buffers = memory_buffers_init(
        NUM_MEMORY_BUFFERS,
        cpu->reg_file,
        cpu->cdb,
        cpu->rob);
    cpu->issue_unit = issue_init(
        cpu->inst_queue,
        cpu->reg_file,
        cpu->alu_res_stations,
        cpu->branch_res_stations,
        cpu->memory_buffers,
        cpu->rob);
    for (uint8_t i = 0; i < NUM_ADDRESS_UNITS; i++)
    {
        cpu->address_units[i] = address_init(
            cpu->memory_buffers,
            cpu->rob);
    }
    for (uint8_t i = 0; i < NUM_ALU_UNITS; i++)
    {
        cpu->alu_units[i] = alu_init(
            cpu->alu_res_stations,
            cpu->reg_file,
            cpu->cdb);
    }
    for (uint8_t i = 0; i < NUM_BRANCH_UNITS; i++)
    {
        cpu->branch_units[i] = branch_init(
            cpu->branch_res_stations,
            cpu->reg_file,
            cpu->cdb,
            cpu->rob);
    }
    for (uint8_t i = 0; i < NUM_MEMORY_UNITS; i++)
    {
        cpu->memory_units[i] = memory_init(
            cpu->memory_buffers,
            cpu->mm,
            cpu->reg_file,
            cpu->cdb);
    }
    cpu->commit_unit = commit_init(
        cpu->rob,
        cpu->mm,
        cpu->reg_file,
        cpu->alu_res_stations,
        cpu->branch_res_stations,
        cpu->memory_buffers,
        cpu->alu_units,
        cpu->branch_units,
        cpu->address_units,
        cpu->memory_units,
        cpu->inst_queue,
        cpu->reg_insts,
        &cpu->pc_src,
        &cpu->reg_pc_target,
        cpu->btb,
        &cpu->jump_zero);

    printf("CPU successfully initialised\n");

    return cpu;
}

void print_main_memory(struct main_memory *mm)
{
    printf("\nHighest %d words of main memory:\n", NUM_WORDS_OUTPUT);
    uint32_t start_index = MEMORY_SIZE - NUM_WORDS_OUTPUT * 4;
    uint32_t initial_offset = start_index % 16;

    for (uint32_t i = start_index; i < MEMORY_SIZE; i += 4)
    {
        printf("%010u: %-11d ", i, main_memory_load_word(mm, i));
        if ((i + 4) % 16 == initial_offset)
        {
            printf("\n");
        }
    }
}

void print_reg_file(struct reg_file *reg_file)
{
    printf("\nRegisters:\n");
    for (int i = 0; i < NUM_REGS; i++)
    {
        printf("x%02d: ", i);
        if (reg_file->regs[i].busy)
        {
            printf("#%-10d ", reg_file->regs[i].rob_id);
        }
        else
        {
            printf("%-11d ", reg_file->regs[i].val);
        }

        if ((i + 1) % 4 == 0)
        {
            printf("\n");
        }
    }
    printf("\n");
}

void cpu_destroy(struct cpu *cpu)
{
    cdb_destroy(cpu->cdb);
    reg_file_destroy(cpu->reg_file);
    main_memory_destroy(cpu->mm);
    fetch_destroy(cpu->fetch_unit);
    decode_destroy(cpu->decode_unit);
    inst_queue_destroy(cpu->inst_queue);
    issue_destroy(cpu->issue_unit);
    res_stations_destroy(cpu->alu_res_stations);
    res_stations_destroy(cpu->branch_res_stations);
    memory_buffers_destroy(cpu->memory_buffers);
    for (uint8_t i = 0; i < NUM_ALU_UNITS; i++)
    {
        alu_destroy(cpu->alu_units[i]);
    }
    for (uint8_t i = 0; i < NUM_BRANCH_UNITS; i++)
    {
        branch_destroy(cpu->branch_units[i]);
    }
    for (uint8_t i = 0; i < NUM_MEMORY_UNITS; i++)
    {
        memory_destroy(cpu->memory_units[i]);
    }
    for (uint8_t i = 0; i < NUM_ADDRESS_UNITS; i++)
    {
        address_destroy(cpu->address_units[i]);
    }
    rob_destroy(cpu->rob);
    commit_destroy(cpu->commit_unit);
    btb_destroy(cpu->btb);

    free(cpu);
}

void update_current(struct cpu *cpu)
{
    cdb_clear(cpu->cdb);

    reg_update_current(&cpu->pc_src);
    reg_update_current(&cpu->reg_pc_target);
    for (uint8_t i = 0; i < ISSUE_WIDTH; i++)
    {
        reg_update_current(&cpu->reg_insts[i]);
        reg_update_current(&cpu->reg_inst_pcs[i]);
        reg_update_current(&cpu->reg_npc_preds[i]);
    }

    inst_queue_update_current(cpu->inst_queue);
    rob_update_current(cpu->rob);

    res_stations_update_current(cpu->alu_res_stations);
    res_stations_update_current(cpu->branch_res_stations);
    memory_buffers_update_current(cpu->memory_buffers);
}

void step(struct cpu *cpu, uint32_t *num_comitted, uint32_t *num_branches, uint32_t *num_mispredicted)
{
    fetch_step(cpu->fetch_unit);
    // printf("PC: %d\n", cpu->fetch_unit->reg_pc);
    decode_step(cpu->decode_unit);
    issue_step(cpu->issue_unit);
    for (uint8_t i = 0; i < NUM_ALU_UNITS; i++)
    {
        alu_step(cpu->alu_units[i]);
    }
    for (uint8_t i = 0; i < NUM_BRANCH_UNITS; i++)
    {
        branch_step(cpu->branch_units[i]);
    }
    for (uint8_t i = 0; i < NUM_MEMORY_UNITS; i++)
    {
        memory_step(cpu->memory_units[i]);
    }
    for (uint8_t i = 0; i < NUM_ADDRESS_UNITS; i++)
    {
        address_step(cpu->address_units[i]);
    }
    res_stations_step(cpu->alu_res_stations);
    res_stations_step(cpu->branch_res_stations);
    memory_buffers_step(cpu->memory_buffers);
    rob_step(cpu->rob);
    commit_step(cpu->commit_unit, num_comitted, num_branches, num_mispredicted);
}

int main(int argc, char *argv[])
{
    if (argc != 2)
    {
        fprintf(stderr, "Usage: %s <file_name>\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    struct cpu *cpu = cpu_init(argv[1]);

    uint32_t num_comitted = 0;
    uint32_t num_branches = 0;
    uint32_t num_mispredicted = 0;
    uint32_t cycles = 0;

    // Cycle until PC will be 0
    while (!cpu->jump_zero)
    {
        // printf("\nCycle: %" PRIu32 "\n", cycles);

        step(cpu, &num_comitted, &num_branches, &num_mispredicted);
        update_current(cpu);

        // print_main_memory(cpu->mm);
        // print_reg_file(cpu->reg_file);

        cycles++;
    }

    print_main_memory(cpu->mm);
    print_reg_file(cpu->reg_file);

    printf("\nInstructions: %" PRIu32 "\n", num_comitted);
    printf("Cycles: %" PRIu32 "\n", cycles);
    printf("IPC: %f\n", (double)num_comitted / cycles);
    printf("Misprediction Rate: %.3f%%\n", (double)num_mispredicted / num_branches * 100);

    cpu_destroy(cpu);
    return 0;
}
