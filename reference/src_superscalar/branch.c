#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "branch.h"
#include "res_stations.h"
#include "reg_file.h"
#include "control.h"
#include "cdb.h"
#include "reg.h"

struct branch_unit *branch_init(
    struct res_stations *branch_res_stations,
    struct reg_file *reg_file,
    struct cdb *cdb,
    struct rob *rob)
{
    struct branch_unit *branch_unit = malloc(sizeof(struct branch_unit));

    if (branch_unit == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for branch unit\n");
        exit(EXIT_FAILURE);
    }

    branch_unit->branch_res_stations = branch_res_stations;
    branch_unit->reg_file = reg_file;
    branch_unit->cdb = cdb;
    branch_unit->rob = rob;

    branch_unit->num_cycles = -1;
    branch_unit->relative_cycle = 0;

    return branch_unit;
}

void branch_step(struct branch_unit *branch_unit)
{
    if (branch_unit->relative_cycle == 0)
    {
        struct res_station *rs_entry = res_stations_remove(branch_unit->branch_res_stations);

        if (rs_entry)
        {
            branch_unit->relative_cycle++;

            uint32_t npc_actual;

            switch (rs_entry->op)
            {
            case JAL:
                npc_actual = rs_entry->a + rs_entry->inst_pc;
                branch_unit->out_cdb = rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_JAL;
                break;
            case JALR:
                npc_actual = (rs_entry->vj + rs_entry->a) & ~1;
                branch_unit->out_cdb = rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_JALR;
                break;
            case BEQ:
                npc_actual = rs_entry->vj == rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BEQ;
                break;
            case BNE:
                npc_actual = rs_entry->vj != rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BNE;
                break;
            case BLT:
                npc_actual = (int32_t)rs_entry->vj < (int32_t)rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BLT;
                break;
            case BLTU:
                npc_actual = rs_entry->vj < rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BLTU;
                break;
            case BGE:
                npc_actual = (int32_t)rs_entry->vj >= (int32_t)rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BGE;
                break;
            case BGEU:
                npc_actual = rs_entry->vj >= rs_entry->vk ? (rs_entry->a + rs_entry->inst_pc) : rs_entry->inst_pc + 4;
                branch_unit->num_cycles = EXEC_CYCLES_BGEU;
                break;
            default:
                fprintf(stderr, "Error: Unknown branch or jump operation\n");
                exit(EXIT_FAILURE);
            }

            branch_unit->entry_is_jump = rs_entry->op == JAL || rs_entry->op == JALR;
            branch_unit->entry_rob_id = rs_entry->rob_id;
            branch_unit->out_npc_actual = npc_actual;
        }
    }
    else if (branch_unit->relative_cycle > 0 && branch_unit->relative_cycle < branch_unit->num_cycles)
    {
        branch_unit->relative_cycle++;
    }
    else if (branch_unit->relative_cycle == branch_unit->num_cycles)
    {
        if (branch_unit->entry_is_jump)
        {
            cdb_write(branch_unit->cdb, branch_unit->entry_rob_id, branch_unit->out_cdb);
        }
        rob_add_npc_actual(branch_unit->rob, branch_unit->entry_rob_id, branch_unit->out_npc_actual);
        branch_unit->relative_cycle = 0;
    }
}

void branch_clear(struct branch_unit *branch_unit)
{
    branch_unit->num_cycles = -1;
    branch_unit->relative_cycle = 0;
}

void branch_destroy(struct branch_unit *branch_unit)
{
    free(branch_unit);
}
