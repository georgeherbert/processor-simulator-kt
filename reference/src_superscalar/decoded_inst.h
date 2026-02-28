#ifndef DECODED_INST_H
#define DECODED_INST_H

#include <stdint.h>

enum op_type
{
    AL,     // Arithmetic or logical operation
    BRANCH, // Branch operation,
    JUMP,   // Jump operation
    LOAD,   // Load operation
    STORE_WORD,
    STORE_HALF,
    STORE_BYTE,
};

enum op
{
    ADDI,
    SLTI,
    SLTIU,
    ANDI,
    ORI,
    XORI,
    SLLI,
    SRLI,
    SRAI,
    LUI,
    AUIPC,
    ADD,
    SLT,
    SLTU,
    AND,
    OR,
    XOR,
    SLL,
    SRL,
    SUB,
    SRA,
    JAL,
    JALR,
    BEQ,
    BNE,
    BLT,
    BLTU,
    BGE,
    BGEU,
    LW,
    LH,
    LHU,
    LB,
    LBU,
    SW,
    SH,
    SB
};

struct decoded_inst
{
    enum op_type op_type;
    enum op op;
    uint8_t rd_addr;
    uint8_t rs1_addr;
    uint8_t rs2_addr;
    uint32_t imm;
    uint32_t inst_pc;
    uint32_t npc_pred;
};

#endif // DECODED_INST_H
