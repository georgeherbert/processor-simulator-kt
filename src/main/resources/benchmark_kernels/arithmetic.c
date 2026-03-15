#include <stdint.h>

int32_t main()
{
    const int32_t num_iterations = 100;
    volatile int32_t runtime_offset = 3;

    int32_t a = 1, b = 2, c = 3, d = 4, e = 5, f = 6, g = 7, h = 8;
    int32_t i = 9, j = 10, k = 11, l = 12, m = 13, n = 14, o = 15, p = 16;
    int32_t q = 17, r = 18, s = 19, t = 20, u = 21, v = 22, w = 23, x = 24, y = 25, z = 26;

    int32_t a_ = 0, b_ = 0, c_ = 0, d_ = 0, e_ = 0, f_ = 0, g_ = 0, h_ = 0;
    int32_t i_ = 0, j_ = 0, k_ = 0, l_ = 0, m_ = 0, n_ = 0;

    for (int32_t iter = 0; iter < num_iterations; ++iter)
    {
        const int32_t mix = (runtime_offset + iter) & 31;
        const int32_t plus = mix & 3;
        const int32_t shift_left = (mix & 1) + 1;
        const int32_t shift_right = ((mix >> 1) & 1) + 1;

        a_ += a + b + plus;
        b_ += c - d - plus;
        c_ += e ^ (f + mix);
        d_ += g | (h + plus);
        e_ += i & (j + mix);
        f_ += k << shift_left;
        g_ += l >> shift_right;
        h_ += m | (n + plus);
        i_ += o ^ (p + mix);
        j_ += q & (r + plus);
        k_ += s | (t + mix);
        l_ += u ^ (v + plus);
        m_ += w & (x + mix);
        n_ += y | (z + plus);

        a += 1;
        b += 2;
        c += 3;
        d += 4;
        e += 5;
        f += 6;
        g += 7;
        h += 8;
        i += 9;
        j += 10;
        k += 11;
        l += 12;
        m += 13;
        n += 14;
        o += 15;
        p += 16;
        q += 17;
        r += 18;
        s += 19;
        t += 20;
        u += 21;
        v += 22;
        w += 23;
        x += 24;
        y += 25;
        z += 26;
    }

    const int32_t result =
        a_ + b_ + c_ + d_ + e_ + f_ + g_ + h_ + i_ + j_ + k_ + l_ + m_ + n_ +
        (((a_ ^ n_) + runtime_offset) & 31);

    return result;
}
