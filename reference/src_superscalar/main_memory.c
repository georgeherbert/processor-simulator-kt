#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "main_memory.h"

uint8_t main_memory_load_byte(struct main_memory *mm, uint32_t addr)
{
    addr %= MEMORY_SIZE; // Speculative execution can cause out of bounds memory accesses
    uint8_t byte = mm->bytes[addr];
    return byte;
}

uint16_t main_memory_load_half(struct main_memory *mm, uint32_t addr)
{
    uint16_t half = 0;
    half |= main_memory_load_byte(mm, addr);
    half |= main_memory_load_byte(mm, addr + 1) << 8;
    return half;
}

uint32_t main_memory_load_word(struct main_memory *mm, uint32_t addr)
{
    uint32_t word = 0;
    word |= main_memory_load_half(mm, addr);
    word |= main_memory_load_half(mm, addr + 2) << 16;
    return word;
}

void main_memory_store_byte(struct main_memory *mm, uint32_t addr, uint8_t val)
{
    mm->bytes[addr] = val;
}

void main_memory_store_half(struct main_memory *mm, uint32_t addr, uint16_t val)
{
    main_memory_store_byte(mm, addr, val & 0xFF);
    main_memory_store_byte(mm, addr + 1, val >> 8);
}

void main_memory_store_word(struct main_memory *mm, uint32_t addr, uint32_t val)
{
    main_memory_store_half(mm, addr, val & 0xFFFF);
    main_memory_store_half(mm, addr + 2, val >> 16);
}

struct main_memory *main_memory_init(char *file_name)
{
    FILE *fp = fopen(file_name, "rb");
    if (fp == NULL)
    {
        fprintf(stderr, "Error: Could not open main memory file %s.\n", file_name);
        exit(EXIT_FAILURE);
    }

    struct main_memory *mm = malloc(sizeof(struct main_memory));
    if (mm == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for main memory.\n");
        exit(EXIT_FAILURE);
    }

    size_t bytes_read = fread(mm->bytes, 1, MEMORY_SIZE, fp);
    if (bytes_read == 0)
    {
        fprintf(stderr, "Error reading from main memory file.\n");
        exit(EXIT_FAILURE);
    }

    printf("Main memory initialised with %zu bytes from %s.\n", bytes_read, file_name);

    return mm;
}

void main_memory_destroy(struct main_memory *mm)
{
    free(mm);
}
