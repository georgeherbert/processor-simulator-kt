#include <stdlib.h>
#include <stdio.h>
#include "address.h"
#include "reg.h"
#include "memory_buffers.h"
#include "rob.h"

struct address_unit *address_init(struct memory_buffers *memory_buffers, struct rob *rob)
{
    struct address_unit *au = malloc(sizeof(struct address_unit));
    if (au == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for address");
        exit(EXIT_FAILURE);
    }

    au->memory_buffers = memory_buffers;
    au->rob = rob;

    au->num_cycles = EXEC_CYCLES_ADDRESS;
    au->relative_cycle = 0;

    return au;
}

void address_step(struct address_unit *au)
{
    if (au->relative_cycle == 0)
    {
        struct memory_buffer *mb_entry = memory_buffers_dequeue_address(au->memory_buffers);
        if (mb_entry)
        {
            au->relative_cycle++;
            au->is_store = mb_entry->op == SW || mb_entry->op == SH || mb_entry->op == SB;
            au->address = mb_entry->vj + mb_entry->a;
            au->dest_id = au->is_store ? mb_entry->rob_id : mb_entry->id;
        }
    }
    else if (au->relative_cycle > 0 && au->relative_cycle < au->num_cycles)
    {
        au->relative_cycle++;
    }
    /*
        The below if an "if" not an "else if" because we don't model the address computation
        to have a "writeback" cycle.
    */
    if (au->relative_cycle == au->num_cycles)
    {
        if (au->is_store)
        {
            rob_add_address(au->rob, au->dest_id, au->address);
            // printf("Store added with address %d\n", au->address);
        }
        else
        {
            memory_buffers_add_address(au->memory_buffers, au->dest_id, au->address);
            // printf("Load added with address %d\n", au->address);
        }
        au->relative_cycle = 0;
    }
}

void address_clear(struct address_unit *au)
{
    au->relative_cycle = 0;
}

void address_destroy(struct address_unit *au)
{
    free(au);
}
