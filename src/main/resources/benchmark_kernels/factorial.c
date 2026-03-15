#include <stdint.h>

uint32_t factorial(uint32_t n);
uint32_t multiply(uint32_t a, uint32_t b);

uint32_t _start()
{
    return factorial(9);
}

uint32_t factorial(uint32_t n)
{
    if (n == 0)
    {
        return 1;
    }
    else
    {
        return multiply(n, factorial(n - 1));
    }
}

uint32_t multiply(uint32_t a, uint32_t b)
{
    uint32_t result = 0;
    for (uint32_t i = 0; i < b; i++)
    {
        result += a;
    }
    return result;
}
