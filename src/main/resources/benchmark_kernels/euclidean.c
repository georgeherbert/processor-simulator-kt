#include <stdint.h>

uint32_t euclidean_algorithm(uint32_t a, uint32_t b);

uint32_t _start()
{
    uint32_t a = 7 * 7 * 7 * 17 * 17 * 17 * 17;
    uint32_t b = 7 * 7 * 7 * 2;

    uint32_t c = euclidean_algorithm(a, b);
    return c;
}

uint32_t euclidean_algorithm(uint32_t a, uint32_t b)
{
    while (a != b)
    {
        if (a > b)
        {
            a = a - b;
        }
        else
        {
            b = b - a;
        }
    }
    return a;
}
