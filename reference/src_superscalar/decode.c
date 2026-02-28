#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "decode.h"
#include "decoded_inst.h"
#include "rv32i.h"
#include "inst_queue.h"
#include "reg.h"
#include "control.h"

#define NA 0

struct decode_unit *decode_init(
    struct reg *reg_inst,
    struct reg *reg_inst_pc,
    struct inst_queue *inst_queue,
    struct reg *reg_npc_pred)
{
    struct decode_unit *decode_unit = malloc(sizeof(struct decode_unit));
    if (decode_unit == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for decode unit\n");
        exit(EXIT_FAILURE);
    }

    decode_unit->reg_inst = reg_inst;
    decode_unit->reg_inst_pc = reg_inst_pc;
    decode_unit->inst_queue = inst_queue;
    decode_unit->reg_npc_pred = reg_npc_pred;

    return decode_unit;
}

/*

Instruction Formats:

R-type
 31                25 24          20 19          15 14    12 11           7 6                  0
+--------------------+--------------+--------------+--------+--------------+--------------------+
| funct7             | rs2          | rs1          | funct3 | rd           | opcode             |
+--------------------+--------------+--------------+--------+--------------+--------------------+

I-type
 31                               20 19          15 14    12 11           7 6                  0
+-----------------------------------+--------------+--------+--------------+--------------------+
| imm[11:0]                         | rs1          | funct3 | rd           | opcode             |
+-----------------------------------+--------------+--------+--------------+--------------------+

S-type
 31                25 24          20 19          15 14    12 11           7 6                  0
+--------------------+--------------+--------------+--------+--------------+--------------------+
| imm[11:5]          | rs2          | rs1          | funct3 | imm[4:0]     | opcode             |
+--------------------+--------------+--------------+--------+--------------+--------------------+

B-type
 31                25 24          20 19          15 14    12 11           7 6                  0
+--------------------+--------------+--------------+--------+--------------+--------------------+
| imm[12|10:5]       | rs2          | rs1          | funct3 | imm[4:1|11]  | opcode             |
+--------------------+--------------+--------------+--------+--------------+--------------------+

U-type
 31                                                       12 11           7 6                  0
+-----------------------------------------------------------+--------------+--------------------+
| imm[31:12]                                                | rd           | opcode             |
+-----------------------------------------------------------+--------------+--------------------+

J-type
 31                                                       12 11           7 6                  0
+-----------------------------------------------------------+--------------+--------------------+
| imm[20|10:1|11|19:12]                                     | rd           | opcode             |
+-----------------------------------------------------------+--------------+--------------------+

*/

uint32_t get_opcode(uint32_t inst)
{
    return inst & 0x7F;
}

uint32_t get_funct3(uint32_t inst)
{
    return (inst >> 12) & 0x7;
}

uint32_t get_funct7(uint32_t inst)
{
    return (inst >> 25) & 0x7F;
}

uint32_t get_rd_addr(uint32_t inst)
{
    return (inst >> 7) & 0x1F;
}

uint32_t get_rs1_addr(uint32_t inst)
{
    return (inst >> 15) & 0x1F;
}

uint32_t get_rs2_addr(uint32_t inst)
{
    return (inst >> 20) & 0x1F;
}

uint32_t get_imm_i(uint32_t inst)
{
    uint32_t imm = (inst >> 20) & 0xFFF;
    if (imm & 0x800)
    {
        imm |= 0xFFFFF000;
    }
    return imm;
}

uint32_t get_imm_s(uint32_t inst)
{
    uint32_t imm = (inst >> 7) & 0x1F;
    imm |= (inst >> 20) & 0xFE0;
    if (imm & 0x800)
    {
        imm |= 0xFFFFF000;
    }
    return imm;
}

uint32_t get_imm_b(uint32_t inst)
{
    uint32_t imm = (inst >> 7) & 0x1E;
    imm |= (inst >> 20) & 0x7E0;
    imm |= (inst << 4) & 0x800;
    imm |= (inst >> 19) & 0x1000;
    if (imm & 0x1000)
    {
        imm |= 0xFFFFE000;
    }
    return imm;
}

uint32_t get_imm_u(uint32_t inst)
{
    return inst & 0xFFFFF000;
}

uint32_t get_imm_j(uint32_t inst)
{
    uint32_t imm = (inst >> 20) & 0x7FE;
    imm |= (inst >> 9) & 0x800;
    imm |= inst & 0xFF000;
    imm |= (inst >> 11) & 0x100000;
    if (imm & 0x100000)
    {
        imm |= 0xFFF00000;
    }
    return imm;
}

void create_decoded_inst(
    struct inst_queue *inst_queue,
    enum op_type op_type,
    enum op op,
    uint8_t rd_addr,
    uint8_t rs1_addr,
    uint8_t rs2_addr,
    uint32_t imm,
    uint32_t inst_pc,
    uint32_t npc_pred)
{
    struct decoded_inst decoded_inst = {
        .op_type = op_type,
        .op = op,
        .rd_addr = rd_addr,
        .rs1_addr = rs1_addr,
        .rs2_addr = rs2_addr,
        .imm = imm,
        .inst_pc = inst_pc,
        .npc_pred = npc_pred};

    inst_queue_enqueue(inst_queue, decoded_inst);
}

void handle_op_imm(struct inst_queue *inst_queue, uint32_t inst)
{
    enum op op;
    uint8_t rd_addr = get_rd_addr(inst);
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint32_t imm = get_imm_i(inst);

    switch (get_funct3(inst))
    {
    case FUNCT3_ADDI:
        op = ADDI;
        // printf("addi x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_SLTI:
        op = SLTI;
        // printf("slti x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_SLTIU:
        op = SLTIU;
        // printf("sltiu x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_XORI:
        op = XORI;
        // printf("xori x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_ORI:
        op = ORI;
        // printf("ori x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_ANDI:
        op = ANDI;
        // printf("andi x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_SLLI:
        op = SLLI;
        imm &= 0x1F;
        // printf("slli x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
        break;
    case FUNCT3_SRLI_SRAI:
        switch (get_funct7(inst))
        {
        case FUNCT7_SRLI:
            op = SRLI;
            imm &= 0x1F;
            // printf("srli x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
            break;
        case FUNCT7_SRAI:
            op = SRAI;
            imm &= 0x1F;
            // printf("srai x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
            break;
        default:
            fprintf(stderr, "Error: Unknown funct7");
            exit(EXIT_FAILURE);
        }
        break;
    default:
        fprintf(stderr, "Error: Unknown funct3");
        exit(EXIT_FAILURE);
    }

    create_decoded_inst(
        inst_queue,
        AL,
        op,
        rd_addr,
        rs1_addr,
        NA,
        imm,
        NA,
        NA);
}

void handle_lui(struct inst_queue *inst_queue, uint32_t inst)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint32_t imm = get_imm_u(inst);

    create_decoded_inst(
        inst_queue,
        AL,
        LUI,
        rd_addr,
        NA,
        NA,
        imm,
        NA,
        NA);

    // printf("lui x%d, %d\n", rd_addr, imm);
}

void handle_auipc(struct inst_queue *inst_queue, uint32_t inst, uint32_t inst_pc)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint32_t imm = get_imm_u(inst);

    create_decoded_inst(
        inst_queue,
        AL,
        AUIPC,
        rd_addr,
        NA,
        NA,
        imm,
        inst_pc,
        NA);

    // printf("auipc x%d, %d", rd_addr, imm);
}

void handle_op(struct inst_queue *inst_queue, uint32_t inst)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint8_t rs2_addr = get_rs2_addr(inst);

    enum op op;

    switch (get_funct3(inst))
    {
    case FUNCT3_ADD_SUB:
        switch (get_funct7(inst))
        {
        case FUNCT7_ADD:
            op = ADD;
            // printf("add x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
            break;
        case FUNCT7_SUB:
            op = SUB;
            // printf("sub x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
            break;
        default:
            fprintf(stderr, "Error: Unknown funct7");
            exit(EXIT_FAILURE);
        }
        break;
    case FUNCT3_SLL:
        op = SLL;
        // printf("sll x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    case FUNCT3_SLT:
        op = SLT;
        // printf("slt x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    case FUNCT3_SLTU:
        op = SLTU;
        // printf("sltu x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    case FUNCT3_XOR:
        op = XOR;
        // printf("xor x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    case FUNCT3_SRL_SRA:
        switch (get_funct7(inst))
        {
        case FUNCT7_SRL:
            op = SRL;
            // printf("srl x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
            break;
        case FUNCT7_SRA:
            op = SRA;
            // printf("sra x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
            break;
        default:
            fprintf(stderr, "Error: Unknown funct7");
            exit(EXIT_FAILURE);
        }
        break;
    case FUNCT3_OR:
        op = OR;
        // printf("or x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    case FUNCT3_AND:
        op = AND;
        // printf("and x%d, x%d, x%d\n", rd_addr, rs1_addr, rs2_addr);
        break;
    default:
        fprintf(stderr, "Error: Unknown funct3");
        exit(EXIT_FAILURE);
    }

    create_decoded_inst(
        inst_queue,
        AL,
        op,
        rd_addr,
        rs1_addr,
        rs2_addr,
        NA,
        NA,
        NA);
}

void handle_jal(struct inst_queue *inst_queue, uint32_t inst, uint32_t inst_pc, uint32_t npc_pred)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint32_t imm = get_imm_j(inst);

    create_decoded_inst(
        inst_queue,
        JUMP,
        JAL,
        rd_addr,
        NA,
        NA,
        imm,
        inst_pc,
        npc_pred);

    // printf("jal x%d, %d\n", rd_addr, imm);
}

void handle_jalr(struct inst_queue *inst_queue, uint32_t inst, uint32_t inst_pc, uint32_t npc_pred)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint32_t imm = get_imm_i(inst);

    create_decoded_inst(
        inst_queue,
        JUMP,
        JALR,
        rd_addr,
        rs1_addr,
        NA,
        imm,
        inst_pc,
        npc_pred);

    // printf("jalr x%d, x%d, %d\n", rd_addr, rs1_addr, imm);
}

void handle_branch(struct inst_queue *inst_queue, uint32_t inst, uint32_t inst_pc, uint32_t npc_pred)
{
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint8_t rs2_addr = get_rs2_addr(inst);
    uint32_t imm = get_imm_b(inst);

    enum op op;

    switch (get_funct3(inst))
    {
    case FUNCT3_BEQ:
        op = BEQ;
        // printf("beq x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    case FUNCT3_BNE:
        op = BNE;
        // printf("bne x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    case FUNCT3_BLT:
        op = BLT;
        // printf("blt x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    case FUNCT3_BLTU:
        op = BLTU;
        // printf("bltu x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    case FUNCT3_BGE:
        op = BGE;
        // printf("bge x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    case FUNCT3_BGEU:
        op = BGEU;
        // printf("bgeu x%d, x%d, %d\n", rs1_addr, rs2_addr, imm);
        break;
    default:
        // printf("Unknown funct3");
        exit(EXIT_FAILURE);
    }

    create_decoded_inst(
        inst_queue,
        BRANCH,
        op,
        NA, // Branch instructions don't use rd
        rs1_addr,
        rs2_addr,
        imm,
        inst_pc,
        npc_pred);
}

void handle_load(struct inst_queue *inst_queue, uint32_t inst)
{
    uint8_t rd_addr = get_rd_addr(inst);
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint32_t imm = get_imm_i(inst);

    enum op op;

    switch (get_funct3(inst))
    {
    case FUNCT3_LB:
        op = LB;
        // printf("lb x%d, %d(x%d)\n", rd_addr, imm, rs1_addr);
        break;
    case FUNCT3_LBU:
        op = LBU;
        // printf("lbu x%d, %d(x%d)\n", rd_addr, imm, rs1_addr);
        break;
    case FUNCT3_LH:
        op = LH;
        // printf("lh x%d, %d(x%d)\n", rd_addr, imm, rs1_addr);
        break;
    case FUNCT3_LHU:
        op = LHU;
        // printf("lhu x%d, %d(x%d)\n", rd_addr, imm, rs1_addr);
        break;
    case FUNCT3_LW:
        op = LW;
        // printf("lw x%d, %d(x%d)\n", rd_addr, imm, rs1_addr);
        break;
    default:
        // printf("Unknown funct3");
        exit(EXIT_FAILURE);
    }

    create_decoded_inst(
        inst_queue,
        LOAD,
        op,
        rd_addr,
        rs1_addr,
        NA,
        imm,
        NA,
        NA);
}

void handle_store(struct inst_queue *inst_queue, uint32_t inst)
{
    uint8_t rs1_addr = get_rs1_addr(inst);
    uint8_t rs2_addr = get_rs2_addr(inst);
    uint32_t imm = get_imm_s(inst);

    // TODO: Tidy this up
    enum op_type op_type;
    enum op op;

    switch (get_funct3(inst))
    {
    case FUNCT3_SB:
        op_type = STORE_BYTE;
        op = SB;
        // printf("sb x%d, %d(x%d)\n", rs2_addr, imm, rs1_addr);
        break;
    case FUNCT3_SH:
        op_type = STORE_HALF;
        op = SH;
        // printf("sh x%d, %d(x%d)\n", rs2_addr, imm, rs1_addr);
        break;
    case FUNCT3_SW:
        op_type = STORE_WORD;
        op = SW;
        // printf("sw x%d, %d(x%d)\n", rs2_addr, imm, rs1_addr);
        break;
    default:
        // printf("Unknown funct3");
        exit(EXIT_FAILURE);
    }

    create_decoded_inst(
        inst_queue,
        op_type,
        op,
        NA,
        rs1_addr,
        rs2_addr,
        imm,
        NA,
        NA);
}

void decode_step(struct decode_unit *decode_unit)
{
    if (inst_queue_free_slots(decode_unit->inst_queue))
    {
        for (uint8_t i = 0; i < ISSUE_WIDTH; i++)
        {
            uint32_t inst_pc = reg_read(&decode_unit->reg_inst_pc[i]);
            uint32_t inst = reg_read(&decode_unit->reg_inst[i]);
            uint32_t opcode = get_opcode(inst);
            uint32_t npc_pred = reg_read(&decode_unit->reg_npc_pred[i]);

            if (inst != 0x0) // Instruction 0x0 indicates fetch unit is stalled
            {
                switch (opcode)
                {
                case OPCODE_OP_IMM:
                    handle_op_imm(decode_unit->inst_queue, inst);
                    break;
                case OPCODE_LUI:
                    handle_lui(decode_unit->inst_queue, inst);
                    break;
                case OPCODE_AUIPC:
                    handle_auipc(decode_unit->inst_queue, inst, inst_pc);
                    break;
                case OPCODE_OP:
                    handle_op(decode_unit->inst_queue, inst);
                    break;
                case OPCODE_JAL:
                    handle_jal(decode_unit->inst_queue, inst, inst_pc, npc_pred);
                    break;
                case OPCODE_JALR:
                    handle_jalr(decode_unit->inst_queue, inst, inst_pc, npc_pred);
                    break;
                case OPCODE_BRANCH:
                    handle_branch(decode_unit->inst_queue, inst, inst_pc, npc_pred);
                    break;
                case OPCODE_LOAD:
                    handle_load(decode_unit->inst_queue, inst);
                    break;
                case OPCODE_STORE:
                    handle_store(decode_unit->inst_queue, inst);
                    break;
                default:
                    fprintf(stderr, "Error: Unknown opcode: %08x", opcode);
                    exit(EXIT_FAILURE);
                }
            }
        }
    }
}

void decode_destroy(struct decode_unit *decode_unit)
{
    free(decode_unit);
}
