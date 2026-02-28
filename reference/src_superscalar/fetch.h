#ifndef FETCH_UNIT_H
#define FETCH_UNIT_H

#include <stdint.h>
#include <stdbool.h>
#include "main_memory.h"
#include "control.h"
#include "inst_queue.h"
#include "reg.h"
#include "btb.h"
#include "config.h"

struct fetch_unit
{
    struct main_memory *mm;        // Pointer to main memory
    struct reg *pc_src;            // Pointer to control signal for PC source
    struct inst_queue *inst_queue; // Pointer to instruction queue
    struct reg *reg_pc_target;     // Pointer to PC target register
    struct reg *reg_insts;         // Pointer to instruction register
    struct reg *reg_inst_pcs;      // Pointer to program counter of instruction in instruction register
    struct reg *reg_npc_preds;     // Pointer to predicted next program counter (for speculative execution)
    struct btb *btb;               // Pointer to branch target buffer
    uint32_t reg_pc;               // Program counter register
    uint32_t reg_npc;              // Next program counter register
};

struct fetch_unit *fetch_init(
    struct main_memory *mm,
    struct reg *pc_src,
    struct inst_queue *inst_queue,
    struct reg *reg_pc_target,
    struct reg *reg_insts,
    struct reg *reg_inst_pcs,
    struct reg *reg_npc_preds,
    struct btb *btb);                              // Initialise fetch unit
void fetch_step(struct fetch_unit *fetch_unit);    // Step fetch unit
void fetch_destroy(struct fetch_unit *fetch_unit); // Free fetch unit

#endif // FETCH_UNIT_H
