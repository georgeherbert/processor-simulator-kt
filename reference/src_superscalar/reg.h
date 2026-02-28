#ifndef REG_H
#define REG_H

#include <stdint.h>

struct reg
{
    uint32_t val_current;
    uint32_t val_next;
};

void reg_write(struct reg *reg, uint32_t val);
uint32_t reg_read(struct reg *reg);
void reg_update_current(struct reg *reg);

#endif // REG_H
