#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <math.h>

#include "btb.h"
#include "config.h"

struct btb *btb_init()
{
    struct btb *btb = malloc(sizeof(struct btb));

    if (btb == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for BTB\n");
        exit(EXIT_FAILURE);
    }

    for (uint32_t i = 0; i < BTB_SIZE; i++)
    {
        btb->entries[i].addr = -1;
        btb->entries[i].bits = pow(2, BTB_BITS - 1); // Weakly not taken
    }

    return btb;
}

uint32_t btb_lookup(struct btb *btb, uint32_t pc)
{
    struct btb_entry btb_entry = btb->entries[(pc / 4) % BTB_SIZE];
    if (btb_entry.addr == pc)
    {
        // return pc + 4;
        // return btb_entry.npc_pred;
        return btb_entry.bits >= pow(2, BTB_BITS - 1) ? btb_entry.npc_pred : pc + 4;
    }
    return pc + 4;
}

void btb_taken(struct btb *btb, uint32_t pc, uint32_t npc_pred)
{
    struct btb_entry *btb_entry = &btb->entries[(pc / 4) % BTB_SIZE];
    if (btb_entry->addr != pc)
    {
        btb_entry->addr = pc;
        btb_entry->bits = pow(2, BTB_BITS - 1);
    }
    else
    {
        btb_entry->bits = btb_entry->bits == pow(2, BTB_BITS) - 1 ? pow(2, BTB_BITS) - 1 : btb_entry->bits + 1;
    }
    btb_entry->npc_pred = npc_pred;
}

void btb_not_taken(struct btb *btb, uint32_t pc)
{
    struct btb_entry *btb_entry = &btb->entries[(pc / 4) % BTB_SIZE];
    if (btb_entry->addr != pc)
    {
        btb_entry->addr = pc;
        btb_entry->bits = pow(2, BTB_BITS - 1);
    }
    else
    {
        btb_entry->bits = btb_entry->bits == 0 ? 0 : btb_entry->bits - 1;
    }
}

void btb_destroy(struct btb *btb)
{
    free(btb);
}
