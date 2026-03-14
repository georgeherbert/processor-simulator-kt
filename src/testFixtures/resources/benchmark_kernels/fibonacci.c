#include <stdint.h>

uint32_t fibonacci(uint8_t n);

uint32_t _start()
{
    return fibonacci(46);
}

uint32_t fibonacci(uint8_t n)
{
    if (n <= 1)
    {
        return n;
    }
    uint32_t fib[n + 1];
    fib[0] = 0;
    fib[1] = 1;
    for (uint8_t i = 2; i <= n; i++)
    {
        fib[i] = fib[i - 1] + fib[i - 2];
    }
    return fib[n];
}
