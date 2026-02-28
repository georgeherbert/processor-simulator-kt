#ifndef BTB_H
#define BTB_H

#include <stdint.h>
#include <stdbool.h>

#include "config.h"

struct btb_entry
{
    uint32_t addr;
    uint8_t bits;
    uint32_t npc_pred;
};

struct btb
{
    struct btb_entry entries[BTB_SIZE];
};

struct btb *btb_init();
uint32_t btb_lookup(struct btb *btb, uint32_t addr);
void btb_taken(struct btb *btb, uint32_t pc, uint32_t npc_pred);
void btb_not_taken(struct btb *btb, uint32_t pc);
void btb_destroy(struct btb *btb);

#endif // BTB_H
