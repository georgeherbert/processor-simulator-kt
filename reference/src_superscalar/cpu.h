#ifndef CPU_H
#define CPU_H

#include <stdint.h>
#include "main_memory.h"
#include "reg_file.h"
#include "fetch.h"
#include "decode.h"
#include "inst_queue.h"
#include "res_stations.h"
#include "issue.h"
#include "alu.h"
#include "branch.h"
#include "memory.h"
#include "control.h"
#include "reg.h"
#include "cdb.h"
#include "rob.h"
#include "memory_buffers.h"
#include "address.h"
#include "commit.h"
#include "btb.h"

struct cpu
{
    struct main_memory *mm;                                // Pointer to main memory
    struct fetch_unit *fetch_unit;                         // Pointer to fetch unit
    struct decode_unit *decode_unit;                       // Pointer to decode unit
    struct inst_queue *inst_queue;                         // Pointer to instruction queue
    struct res_stations *alu_res_stations;                 // Pointer to ALU reservation station
    struct res_stations *branch_res_stations;              // Pointer to branch reservation station
    struct memory_buffers *memory_buffers;                 // Pointer to memory reservation station
    struct address_unit *address_units[NUM_ADDRESS_UNITS]; // Pointer to address unit
    struct issue_unit *issue_unit;                         // Pointer to issue unit
    struct alu_unit *alu_units[NUM_ALU_UNITS];             // Pointer to ALU units
    struct branch_unit *branch_units[NUM_BRANCH_UNITS];    // Pointer to branch unit
    struct memory_unit *memory_units[NUM_MEMORY_UNITS];    // Pointer to memory unit
    struct cdb *cdb;                                       // Pointer to common data bus
    struct rob *rob;                                       // Pointer to reorder buffer
    struct commit_unit *commit_unit;                       // Pointer to commit unit
    struct btb *btb;                                       // Pointer to branch target buffer

    struct reg_file *reg_file;             // Pointer to register file
    struct reg pc_src;                     // Control signal for PC source
    struct reg reg_pc_target;              // PC target from branch unit
    struct reg reg_insts[ISSUE_WIDTH];     // Instruction register
    struct reg reg_inst_pcs[ISSUE_WIDTH];  // Program counter of instruction in instruction register
    struct reg reg_npc_preds[ISSUE_WIDTH]; // Predicted next program counter (for speculative execution)

    bool jump_zero; // Indicates whether we have had a jump to zero (i.e. final instruction)
};

struct cpu *cpu_init(char *file_name); // Initialise cpu
void cpu_destroy(struct cpu *cpu);     // Destroy relevent parts of cpu

#endif // CPU_H
