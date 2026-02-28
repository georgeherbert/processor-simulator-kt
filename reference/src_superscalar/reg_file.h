#ifndef REG_FILE_H
#define REG_FILE_H

#include <stdint.h>
#include <stdbool.h>
#include "cdb.h"

#define NUM_REGS 32

struct reg_file_entry
{
    uint32_t val;    // val of register (if not in reservation station)
    bool busy;       // Whether register value is in reservation station
    uint32_t rob_id; // Reorder buffer entry ID
};

struct reg_file
{
    struct reg_file_entry regs[NUM_REGS];
    struct cdb *cdb; // Pointer to common data bus
};

struct reg_file *reg_file_init(struct cdb *cdb);                                                       // Initialise register file
bool reg_file_get_reg_busy(struct reg_file *reg_file, uint32_t reg_addr);                              // Get busy bit of register
uint32_t reg_file_get_rob_id(struct reg_file *reg_file, uint32_t reg_addr);                            // Get ROB ID of register
uint32_t reg_file_get_reg_val(struct reg_file *reg_file, uint32_t reg_addr);                           // Get val of register
void reg_file_set_rob_id(struct reg_file *reg_file, uint32_t reg_addr, uint32_t rob_id);               // Set ROB ID of register
void reg_file_reg_commit(struct reg_file *reg_file, uint32_t reg_addr, uint32_t val, uint32_t rob_id); // Commit register
void reg_file_clear(struct reg_file *reg_file);                                                        // Clear register file on mispredict
void reg_file_destroy(struct reg_file *reg_file);                                                      // Free register file

#endif // REG_FILE_H
