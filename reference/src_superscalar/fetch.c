#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "fetch.h"
#include "main_memory.h"
#include "control.h"
#include "inst_queue.h"
#include "reg.h"

struct fetch_unit *fetch_init(
    struct main_memory *mm,
    struct reg *pc_src,
    struct inst_queue *inst_queue,
    struct reg *reg_pc_target,
    struct reg *reg_insts,
    struct reg *reg_inst_pcs,
    struct reg *reg_npc_preds,
    struct btb *btb)
{
    struct fetch_unit *fetch_unit = malloc(sizeof(struct fetch_unit));
    if (fetch_unit == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for fetch unit\n");
        exit(EXIT_FAILURE);
    }

    fetch_unit->mm = mm;
    fetch_unit->pc_src = pc_src;
    fetch_unit->inst_queue = inst_queue;
    fetch_unit->reg_pc_target = reg_pc_target;
    fetch_unit->reg_insts = reg_insts;
    fetch_unit->reg_inst_pcs = reg_inst_pcs;
    fetch_unit->reg_npc_preds = reg_npc_preds;
    fetch_unit->btb = btb;
    fetch_unit->reg_pc = 0;
    fetch_unit->reg_npc = 0;

    return fetch_unit;
}

void fetch_step(struct fetch_unit *fetch_unit)
{
    if (inst_queue_free_slots(fetch_unit->inst_queue))
    {
        if (reg_read(fetch_unit->pc_src) == PC_SRC_NORMAL)
        {
            fetch_unit->reg_pc = fetch_unit->reg_npc;
        }
        else if (reg_read(fetch_unit->pc_src) == PC_SRC_MISPREDICT)
        {
            fetch_unit->reg_pc = reg_read(fetch_unit->reg_pc_target);
            reg_write(fetch_unit->pc_src, PC_SRC_NORMAL);
        }
        else
        {
            fprintf(stderr, "Error: Invalid PC source control signal %d\n", reg_read(fetch_unit->pc_src));
            exit(EXIT_FAILURE);
        }
        uint32_t npc;
        bool branch_taken = false;
        for (uint8_t i = 0; i < ISSUE_WIDTH; i++)
        {
            uint32_t pc = fetch_unit->reg_pc + i * 4;
            uint32_t inst = main_memory_load_word(fetch_unit->mm, pc);
            npc = branch_taken ? npc : btb_lookup(fetch_unit->btb, pc);

            reg_write(&fetch_unit->reg_inst_pcs[i], pc);
            reg_write(&fetch_unit->reg_npc_preds[i], npc);
            reg_write(&fetch_unit->reg_insts[i], branch_taken ? 0x0 : inst);

            branch_taken |= npc != pc + 4;
        }
        fetch_unit->reg_npc = npc;
    }
}

void fetch_destroy(struct fetch_unit *fetch_unit)
{
    free(fetch_unit);
}
