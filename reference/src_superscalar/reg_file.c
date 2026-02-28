#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include "reg_file.h"
#include "cpu.h"
#include "rob.h"

#define NA 0

struct reg_file *reg_file_init(struct cdb *cdb)
{
    struct reg_file *reg_file = malloc(sizeof(struct reg_file));
    if (reg_file == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for register file");
        exit(EXIT_FAILURE);
    }

    for (uint32_t i = 0; i < NUM_REGS; i++)
    {
        reg_file->regs[i].val = 0;      // All vals are initially zero
        reg_file->regs[i].busy = false; // All vals are not initially in the reorder buffer
    }

    reg_file->regs[8].val = MEMORY_SIZE;
    reg_file->regs[2].val = MEMORY_SIZE;

    reg_file->cdb = cdb;

    return reg_file;
}

bool reg_file_get_reg_busy(struct reg_file *reg_file, uint32_t reg_addr)
{
    return reg_file->regs[reg_addr].busy;
}

uint32_t reg_file_get_rob_id(struct reg_file *reg_file, uint32_t reg_addr)
{
    return reg_file->regs[reg_addr].rob_id;
}

uint32_t reg_file_get_reg_val(struct reg_file *reg_file, uint32_t reg_addr)
{
    return reg_file->regs[reg_addr].val;
}

void reg_file_reg_commit(struct reg_file *reg_file, uint32_t reg_addr, uint32_t val, uint32_t rob_id)
{
    if (reg_addr != 0)
    {
        // printf("\t%d %d\n", reg_addr, val);
        if (reg_file->regs[reg_addr].rob_id == rob_id)
        {
            reg_file->regs[reg_addr].busy = false;
        }
        reg_file->regs[reg_addr].val = val;
    }
}

void reg_file_set_rob_id(struct reg_file *reg_file, uint32_t reg_addr, uint32_t rob_id)
{
    // Register zero is always zero and cannot be written to
    if (reg_addr != 0)
    {
        reg_file->regs[reg_addr].busy = true;
        reg_file->regs[reg_addr].rob_id = rob_id;
    }
}

void reg_file_clear(struct reg_file *reg_file)
{
    for (uint32_t i = 0; i < NUM_REGS; i++)
    {
        reg_file->regs[i].busy = false;
    }
}

void reg_file_destroy(struct reg_file *reg_file)
{
    free(reg_file);
}
