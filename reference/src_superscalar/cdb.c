#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include "cdb.h"

struct cdb *cdb_init(uint32_t num_entries)
{
    struct cdb *cdb = malloc(sizeof(struct cdb));
    if (cdb == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for common data bus");
        exit(EXIT_FAILURE);
    }

    cdb->entries = malloc(sizeof(struct cdb_entry) * num_entries);
    if (cdb->entries == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for common data bus entries");
        exit(EXIT_FAILURE);
    }

    for (uint32_t i = 0; i < num_entries; i++)
    {
        cdb->entries[i].rob_id = 0;
        cdb->entries[i].val = 0;
    }

    cdb->num_entries = num_entries;

    return cdb;
}

void cdb_write(struct cdb *cdb, uint32_t rob_id, uint32_t val)
{
    for (uint32_t i = 0; i < cdb->num_entries; i++)
    {
        if (cdb->entries[i].rob_id == 0)
        {
            // printf("CDB #%d: %d\n", rob_id, val);
            cdb->entries[i].rob_id = rob_id;
            cdb->entries[i].val = val;
            return;
        }
    }
}

bool cdb_is_val_ready(struct cdb *cdb, uint32_t rob_id)
{
    for (uint32_t i = 0; i < cdb->num_entries; i++)
    {
        if (cdb->entries[i].rob_id == rob_id)
        {
            return true;
        }
    }

    return false;
}

uint32_t cdb_get_val(struct cdb *cdb, uint32_t rob_id)
{
    for (uint32_t i = 0; i < cdb->num_entries; i++)
    {
        if (cdb->entries[i].rob_id == rob_id)
        {
            return cdb->entries[i].val;
        }
    }
    fprintf(stderr, "Error: Could not find value for rob_id %d in common data bus", rob_id);
    exit(EXIT_FAILURE);
}

void cdb_clear(struct cdb *cdb)
{
    for (uint32_t i = 0; i < cdb->num_entries; i++)
    {
        cdb->entries[i].rob_id = 0;
    }
}

void cdb_destroy(struct cdb *cdb)
{
    free(cdb->entries);
    free(cdb);
}
