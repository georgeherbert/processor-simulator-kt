#ifndef COMMIT_H
#define COMMIT_H

#include "rob.h"
#include "reg.h"
#include "main_memory.h"
#include "reg_file.h"
#include "res_stations.h"
#include "memory_buffers.h"
#include "alu.h"
#include "branch.h"
#include "inst_queue.h"
#include "btb.h"
#include "address.h"
#include "memory.h"

struct commit_unit
{
    struct rob *rob;
    struct main_memory *mm;
    struct reg_file *reg_file;
    struct res_stations *alu_rs;
    struct res_stations *branch_rs;
    struct memory_buffers *mb;
    struct alu_unit **alus;
    struct branch_unit **branch_units;
    struct address_unit **address_units;
    struct memory_unit **memory_units;
    struct inst_queue *iq;
    struct reg *reg_insts;
    struct reg *pc_src;        // Pointer to control signal for PC source
    struct reg *reg_pc_target; // Pointer to PC target register
    struct btb *btb;           // Pointer to branch target buffer
    bool *jump_zero;
};

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
    bool *jump_zero);
void commit_step(struct commit_unit *commit_unit, uint32_t *num_committed, uint32_t *num_branches, uint32_t *num_mispredicted);
void commit_destroy(struct commit_unit *commit_unit);

#endif // COMMIT_H
