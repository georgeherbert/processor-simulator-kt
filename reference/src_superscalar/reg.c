#include <stdint.h>
#include "reg.h"

void reg_write(struct reg *reg, uint32_t val)
{
    reg->val_next = val;
}

uint32_t reg_read(struct reg *reg)
{
    return reg->val_current;
}

void reg_update_current(struct reg *reg)
{
    reg->val_current = reg->val_next;
}
