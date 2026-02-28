#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include "inst_queue.h"
#include "decode.h"
#include "config.h"

struct inst_queue *inst_queue_init()
{
    struct inst_queue *iq = malloc(sizeof(struct inst_queue));
    if (iq == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for instruction queue\n");
        exit(EXIT_FAILURE);
    }

    iq->front_current = -1;
    iq->front_next = -1;
    iq->rear_current = -1;
    iq->rear_next = -1;

    return iq;
}

void inst_queue_enqueue(struct inst_queue *iq, struct decoded_inst inst)
{
    if (iq->front_next == -1)
    {
        iq->front_next = 0;
    }
    iq->rear_next = (iq->rear_next + 1) % INST_QUEUE_SIZE;
    iq->queue_next[iq->rear_next] = inst;
}

struct decoded_inst inst_queue_dequeue(struct inst_queue *iq, uint32_t batch_num)
{
    struct decoded_inst inst = iq->queue_current[iq->front_current + batch_num];
    if (iq->front_next == iq->rear_next)
    {
        iq->front_next = -1;
        iq->rear_next = -1;
    }
    else
    {
        iq->front_next = (iq->front_next + 1) % INST_QUEUE_SIZE;
    }
    return inst;
}

bool inst_queue_free_slots(struct inst_queue *iq)
{
    if (iq->front_current == -1 && iq->rear_current == -1)
    {
        return true;
    }

    int32_t spaces_free = iq->front_current - iq->rear_current - 1;
    if (spaces_free < 0)
    {
        spaces_free += INST_QUEUE_SIZE;
    }
    return spaces_free >= ISSUE_WIDTH;
}

uint32_t inst_queue_cur_entries(struct inst_queue *iq)
{
    uint32_t queue_size = (iq->front_current == -1)                 ? 0
                          : (iq->front_current <= iq->rear_current) ? iq->rear_current - iq->front_current + 1
                                                                    : (INST_QUEUE_SIZE - iq->front_current) + iq->rear_current + 1;
    return queue_size;
}

void inst_queue_clear(struct inst_queue *iq)
{
    iq->front_next = -1;
    iq->rear_next = -1;
}

void inst_queue_update_current(struct inst_queue *iq)
{
    memcpy(iq->queue_current, iq->queue_next, sizeof(struct decoded_inst) * INST_QUEUE_SIZE);
    iq->front_current = iq->front_next;
    iq->rear_current = iq->rear_next;
}

void inst_queue_destroy(struct inst_queue *inst_queue)
{
    free(inst_queue);
}
