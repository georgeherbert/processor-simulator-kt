#include <stdint.h>

uint32_t divide(uint32_t dividend, uint32_t divisor);

uint32_t _start()
{
    return divide(4294967295, 100000000);
}

uint32_t divide(uint32_t dividend, uint32_t divisor)
{
    if (divisor == 0)
    {
        return 0;
    }

    uint32_t quotient = 0;
    uint32_t remainder = 0;

    for (int32_t i = 31; i >= 0; i--)
    {
        remainder <<= 1;
        remainder |= (dividend >> i) & 1;

        if (remainder >= divisor)
        {
            remainder -= divisor;
            quotient |= (1 << i);
        }
    }

    return quotient;
}
