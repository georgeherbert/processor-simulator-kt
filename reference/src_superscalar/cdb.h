#ifndef CDB_H
#define CDB_H

#include <stdint.h>
#include <stdbool.h>

struct cdb_entry
{
    uint32_t rob_id; // Reorder buffer ID of result
    uint32_t val;    // val of result
};

struct cdb
{
    struct cdb_entry *entries; // Array of entries
    uint32_t num_entries;      // Number of entries (should be equal to the number of functional units)
};

struct cdb *cdb_init(uint32_t num_entries);                     // Initialise common data bus
void cdb_write(struct cdb *cdb, uint32_t rob_id, uint32_t val); // Put val going to ROB ID on common data bus
bool cdb_is_val_ready(struct cdb *cdb, uint32_t rob_id);        // Check if val going to ROB ID is on common data bus
uint32_t cdb_get_val(struct cdb *cdb, uint32_t rob_id);         // Get val going to ROB ID from common data bus
void cdb_clear(struct cdb *cdb);                                // Step common data bus
void cdb_destroy(struct cdb *cdb);                              // Free common data bus

#endif // CDB_H
