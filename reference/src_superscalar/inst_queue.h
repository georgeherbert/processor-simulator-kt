#ifndef INST_QUEUE_H
#define INST_QUEUE_H

#include <stdbool.h>
#include "decoded_inst.h"
#include "reg.h"
#include "config.h"

struct inst_queue
{
    struct decoded_inst queue_current[INST_QUEUE_SIZE];
    struct decoded_inst queue_next[INST_QUEUE_SIZE];
    int32_t front_current;
    int32_t front_next;
    int32_t rear_next;
    int32_t rear_current;
};

struct inst_queue *inst_queue_init();                                              // Initialise instruction queue
void inst_queue_enqueue(struct inst_queue *inst_queue, struct decoded_inst inst);  // Push instruction to queue
bool inst_queue_free_slots(struct inst_queue *iq);                                 // Check if instruction queue is full
uint32_t inst_queue_cur_entries(struct inst_queue *iq);                            // Get number of instructions in queue
struct decoded_inst inst_queue_dequeue(struct inst_queue *iq, uint32_t batch_num); // Pop batch_num-th instruction from queue
void inst_queue_clear(struct inst_queue *inst_queue);                              // Clear instruction queue on mispredict
void inst_queue_update_current(struct inst_queue *inst_queue);                     // Set current instruction queue to next instruction queue
void inst_queue_destroy(struct inst_queue *inst_queue);                            // Free instruction queue

#endif // INST_QUEUE_H
