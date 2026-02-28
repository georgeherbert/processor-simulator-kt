#ifndef MEMORY_H
#define MEMORY_H

#include "reg_file.h"
#include "main_memory.h"
#include "cdb.h"
#include "reg.h"
#include "memory_buffers.h"

struct memory_unit
{
    struct memory_buffers *memory_buffers; // Pointer to memory buffers
    struct main_memory *mm;                // Pointer to main memory
    struct reg_file *reg_file;             // Pointer to register file
    struct cdb *cdb;                       // Pointer to common data bus

    uint32_t num_cycles;
    uint32_t relative_cycle;
    uint32_t entry_rob_id;
    uint32_t out;
};

struct memory_unit *memory_init(
    struct memory_buffers *memory_buffers,
    struct main_memory *mm,
    struct reg_file *reg_file,
    struct cdb *cdb);                                 // Initialise memory unit
void memory_step(struct memory_unit *memory_unit);    // Step memory unit
void memory_clear(struct memory_unit *mu);            // Clear memory unit on mispredict
void memory_destroy(struct memory_unit *memory_unit); // Free memory unit

#endif // MEMORY_H
