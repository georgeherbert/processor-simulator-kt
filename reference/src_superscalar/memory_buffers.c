#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include "memory_buffers.h"
#include "reg_file.h"
#include "reg.h"
#include "decoded_inst.h"

struct memory_buffers *memory_buffers_init(
    uint32_t num_buffers,
    struct reg_file *reg_file,
    struct cdb *cdb,
    struct rob *rob)
{
    struct memory_buffers *mb = malloc(sizeof(struct memory_buffers));
    if (mb == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for memory buffers");
        exit(EXIT_FAILURE);
    }

    mb->buffers_current = malloc(sizeof(struct memory_buffer) * num_buffers);
    if (mb->buffers_current == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for current individual memory buffers");
        exit(EXIT_FAILURE);
    }

    mb->buffers_next = malloc(sizeof(struct memory_buffer) * num_buffers);
    if (mb->buffers_next == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for next individual memory buffers");
        exit(EXIT_FAILURE);
    }

    mb->num_buffers = num_buffers;
    mb->reg_file = reg_file;
    mb->cdb = cdb;
    mb->num_buffers_in_queue_current = 0;
    mb->num_buffers_in_queue_next = 0;
    mb->rob = rob;

    for (uint32_t i = 0; i < num_buffers; i++)
    {
        mb->buffers_current[i].id = i;
        mb->buffers_current[i].busy = false;
        mb->buffers_next[i].id = i;
        mb->buffers_next[i].busy = false;
        mb->buffers_current[i].queue_pos = 0;
        mb->buffers_next[i].queue_pos = 0;
    }

    mb->cur_cycle_count_address = 0;
    mb->cur_cycle_count_memory = 0;
    mb->cur_cycle_count_enqueue = 0;

    return mb;
}

void memory_buffers_step(struct memory_buffers *mb)
{
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        if (mb->buffers_next[i].busy)
        {
            if (mb->buffers_next[i].qj != 0)
            {
                if (cdb_is_val_ready(mb->cdb, mb->buffers_next[i].qj))
                {
                    mb->buffers_next[i].vj = cdb_get_val(mb->cdb, mb->buffers_next[i].qj);
                    mb->buffers_next[i].qj = 0;
                }
            }
            if (mb->buffers_next[i].qk != 0)
            {
                if (cdb_is_val_ready(mb->cdb, mb->buffers_next[i].qk))
                {
                    mb->buffers_next[i].vk = cdb_get_val(mb->cdb, mb->buffers_next[i].qk);
                    mb->buffers_next[i].qk = 0;
                }
            }
        }
    }
}

void memory_buffers_enqueue(
    struct memory_buffers *mb,
    enum op op,
    uint32_t qj,
    uint32_t qk,
    uint32_t vj,
    uint32_t vk,
    uint32_t a,
    uint32_t rob_id)
{
    uint32_t id = mb->cur_cycle_count_enqueue;
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        if (!mb->buffers_current[i].busy)
        {
            if (id == 0)
            {
                mb->buffers_next[i].busy = true;
                mb->buffers_next[i].op = op;
                mb->buffers_next[i].qj = qj;
                mb->buffers_next[i].qk = qk;
                mb->buffers_next[i].vj = vj;
                mb->buffers_next[i].vk = vk;
                mb->buffers_next[i].a = a;
                mb->buffers_next[i].rob_id = rob_id;
                /*
                    The +1 in the next line ensures that the queue position is never 0.
                    This is because 0 is used to indicate that the buffer is not in the queue.
                */
                mb->buffers_next[i].queue_pos = mb->num_buffers_in_queue_current + 1 + mb->cur_cycle_count_enqueue;
                mb->num_buffers_in_queue_next++;
                mb->cur_cycle_count_enqueue++;
                break;
            }
            id--;
        }
    }
}

uint32_t memory_buffers_num_free(struct memory_buffers *mb)
{
    uint32_t num_free = 0;
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        num_free += !mb->buffers_current[i].busy;
    }
    return num_free;
}

struct memory_buffer *memory_buffers_dequeue_memory(struct memory_buffers *mb)
{
    uint32_t id = mb->cur_cycle_count_memory;
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        struct memory_buffer *mb_entry = &mb->buffers_current[i];
        enum op op = mb_entry->op;
        bool is_load = op == LW || op == LH || op == LHU || op == LB || op == LBU;
        if (is_load && mb_entry->busy && mb_entry->queue_pos == 0 && mb_entry->a != 0)
        {
            // We nest this if statement for efficiency
            if (!rob_earlier_stores(mb->rob, mb_entry->rob_id, mb_entry->a))
            {
                if (id == 0)
                {
                    mb->buffers_next[i].busy = false;
                    mb->cur_cycle_count_memory++;
                    // printf("%d\n", mb_entry->a);
                    return mb_entry;
                }
                id--;
            }
        }
    }
    return NULL;
}

void get_queue_pos_indices(struct memory_buffers *mb, uint32_t *queue_pos_indices)
{
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        if (mb->buffers_current[i].busy && mb->buffers_current[i].queue_pos != 0)
        {
            queue_pos_indices[mb->buffers_current[i].queue_pos - 1] = i;
        }
    }
}

void shift_queue(struct memory_buffers *mb, uint32_t removed_pos)
{
    mb->num_buffers_in_queue_next--;
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        if (mb->buffers_next[i].busy && mb->buffers_next[i].queue_pos > removed_pos)
        {
            mb->buffers_next[i].queue_pos--;
        }
    }
}

struct memory_buffer *memory_buffers_dequeue_address(struct memory_buffers *mb)
{
    uint32_t id = mb->cur_cycle_count_address;
    uint32_t queue_pos_indices[mb->num_buffers_in_queue_current];
    get_queue_pos_indices(mb, queue_pos_indices);

    // printf("%d\n", mb->num_buffers_in_queue_current);
    bool raw_potential = false;
    /*
        We can't send a load for effective address execution if we haven't sent any prior stores.
        Otherwise, a load could then execute in the memory stage before the store, even though
        they may both have the same effective address. This would be a RAW hazard.
    */
    // printf("\n");
    for (uint32_t i = 0; i < mb->num_buffers_in_queue_current; i++)
    {
        struct memory_buffer *mb_entry = &mb->buffers_current[queue_pos_indices[i]];
        if (mb_entry->op == SW || mb_entry->op == SH || mb_entry->op == SB)
        {
            if (mb_entry->qj == 0)
            {
                if (id == 0)
                {
                    // printf("STORE\n");
                    mb->buffers_next[queue_pos_indices[i]].busy = false;
                    shift_queue(mb, mb->buffers_next[queue_pos_indices[i]].queue_pos);
                    mb->cur_cycle_count_address++;
                    return mb_entry;
                }
                id--;
            }
            else
            {
                raw_potential = true;
            }
        }
        else if (!raw_potential)
        {
            if (mb_entry->qj == 0)
            {
                if (id == 0)
                {
                    // printf("LOAD\n");
                    shift_queue(mb, mb->buffers_next[queue_pos_indices[i]].queue_pos);
                    /*
                        We set the address to 0 to indicate that the buffer is in the address unit.
                    */
                    mb->buffers_next[queue_pos_indices[i]].a = 0;
                    mb->buffers_next[queue_pos_indices[i]].queue_pos = 0;
                    mb->cur_cycle_count_address++;
                    return mb_entry;
                }
                id--;
            }
        }
    }
    return NULL;
}

void memory_buffers_add_address(struct memory_buffers *mb, uint32_t id, uint32_t address)
{
    mb->buffers_next[id].a = address;
    mb->buffers_next[id].queue_pos = 0;
}

void memory_buffers_clear(struct memory_buffers *mb)
{
    for (uint32_t i = 0; i < mb->num_buffers; i++)
    {
        mb->buffers_next[i].busy = false;
    }
    mb->num_buffers_in_queue_next = 0;
}

void memory_buffers_update_current(struct memory_buffers *mb)
{
    memcpy(mb->buffers_current, mb->buffers_next, sizeof(struct memory_buffer) * mb->num_buffers);
    mb->num_buffers_in_queue_current = mb->num_buffers_in_queue_next;
    mb->cur_cycle_count_address = 0;
    mb->cur_cycle_count_memory = 0;
    mb->cur_cycle_count_enqueue = 0;
}

void memory_buffers_destroy(struct memory_buffers *mb)
{
    free(mb->buffers_current);
    free(mb->buffers_next);
    free(mb);
}
