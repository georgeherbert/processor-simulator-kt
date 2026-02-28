#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include "issue.h"
#include "inst_queue.h"
#include "res_stations.h"
#include "reg_file.h"
#include "decode.h"
#include "decoded_inst.h"
#include "memory_buffers.h"
#include "rob.h"

#define NA 0
#define min(a, b) (((a) < (b)) ? (a) : (b))

struct dependency
{
    uint32_t rd_addr;
    uint32_t rob_id;
    bool busy;
};

struct issue_unit *issue_init(
    struct inst_queue *inst_queue,
    struct reg_file *reg_file,
    struct res_stations *alu_res_stations,
    struct res_stations *branch_res_stations,
    struct memory_buffers *memory_buffers,
    struct rob *rob)
{
    struct issue_unit *issue_unit = malloc(sizeof(struct issue_unit));

    if (issue_unit == NULL)
    {
        fprintf(stderr, "Error: Could not allocate memory for issue unit\n");
        exit(EXIT_FAILURE);
    }

    issue_unit->inst_queue = inst_queue;
    issue_unit->reg_file = reg_file;
    issue_unit->alu_res_stations = alu_res_stations;
    issue_unit->branch_res_stations = branch_res_stations;
    issue_unit->memory_buffers = memory_buffers;
    issue_unit->rob = rob;

    return issue_unit;
}

void add_dependency(struct dependency *dependencies, uint32_t batch_size, uint32_t rd_addr, uint32_t rob_id)
{
    if (rd_addr != 0)
    {
        for (uint32_t i = 0; i < batch_size; i++)
        {
            if (!dependencies[i].busy || (dependencies[i].busy && dependencies[i].rd_addr == rd_addr))
            {
                dependencies[i].rd_addr = rd_addr;
                dependencies[i].rob_id = rob_id;
                dependencies[i].busy = true;
                break;
            }
        }
    }
}

uint32_t get_dependency(struct dependency *dependencies, uint32_t batch_size, uint32_t rs_addr)
{
    for (uint32_t i = 0; i < batch_size; i++)
    {
        if (dependencies[i].busy && dependencies[i].rd_addr == rs_addr)
        {
            return dependencies[i].rob_id;
        }
    }
    return NA;
}

void set_q_v(struct reg_file *reg_file, struct rob *rob, uint32_t rs_addr, uint32_t *q, uint32_t *v, struct dependency *dependencies, uint32_t batch_size)
{
    uint32_t dependency = get_dependency(dependencies, batch_size, rs_addr);
    if (dependency)
    {
        *q = dependency;
        *v = NA;
    }
    else if (reg_file_get_reg_busy(reg_file, rs_addr))
    {
        uint32_t rob_id = reg_file_get_rob_id(reg_file, rs_addr);
        if (rob_is_entry_ready(rob, rob_id))
        {
            *q = NA;
            *v = rob_get_entry_value(rob, rob_id);
        }
        else
        {
            *q = rob_id;
            *v = NA;
        }
    }
    else
    {
        *q = NA;
        *v = reg_file_get_reg_val(reg_file, rs_addr);
    }
}

void handle_al_operation(
    struct decoded_inst inst,
    struct rob *rob,
    struct reg_file *reg_file,
    struct res_stations *alu_res_stations,
    struct dependency *dependencies,
    uint32_t batch_size)
{
    uint32_t dest_rob_id = rob_enqueue(rob, inst.op_type, inst.rd_addr, NA, NA, NA, NA);

    uint32_t qj = NA;
    uint32_t qk = NA;
    uint32_t vj = NA;
    uint32_t vk = NA;
    uint32_t inst_pc = NA;

    switch (inst.op)
    {
    case ADDI:
    case SLTI:
    case SLTIU:
    case ANDI:
    case ORI:
    case XORI:
    case SLLI:
    case SRLI:
    case SRAI:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        vk = inst.imm;
        break;
    case LUI:
        vj = inst.imm;
        break;
    case AUIPC:
        vj = inst.imm;
        inst_pc = inst.inst_pc;
        break;
    case ADD:
    case SLT:
    case SLTU:
    case AND:
    case OR:
    case XOR:
    case SLL:
    case SRL:
    case SUB:
    case SRA:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        set_q_v(reg_file, rob, inst.rs2_addr, &qk, &vk, dependencies, batch_size);
        break;
    default:
        fprintf(stderr, "Error: Unknown arithmetic or logical op %d", inst.op);
        exit(EXIT_FAILURE);
        break;
    }

    add_dependency(dependencies, batch_size, inst.rd_addr, dest_rob_id);

    // printf("ALU; ROB: %d; QJ: %d; VJ: %d; QK: %d; VK: %d\n", dest_rob_id, qj, vj, qk, vk);

    res_stations_add(
        alu_res_stations,
        inst.op,
        qj,
        qk,
        vj,
        vk,
        NA,
        dest_rob_id,
        inst_pc);

    reg_file_set_rob_id(reg_file, inst.rd_addr, dest_rob_id);
}

void handle_branch_operation(
    struct decoded_inst inst,
    struct rob *rob,
    struct reg_file *reg_file,
    struct res_stations *branch_res_stations,
    struct dependency *dependencies,
    uint32_t batch_size)
{
    /*
        Branch instructions have no rd_addr.
        But rd_addr should be NA from the decode unit for branch instructions.
    */
    uint32_t dest_rob_id = rob_enqueue(rob, inst.op_type, inst.rd_addr, NA, NA, inst.npc_pred, inst.inst_pc);

    uint32_t qj = NA;
    uint32_t qk = NA;
    uint32_t vj = NA;
    uint32_t vk = NA;

    switch (inst.op)
    {
    case JAL:
        reg_file_set_rob_id(reg_file, inst.rd_addr, dest_rob_id);
        add_dependency(dependencies, batch_size, inst.rd_addr, dest_rob_id);
        break;
    case JALR:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        reg_file_set_rob_id(reg_file, inst.rd_addr, dest_rob_id);
        add_dependency(dependencies, batch_size, inst.rd_addr, dest_rob_id);
        break;
    case BEQ:
    case BNE:
    case BLT:
    case BLTU:
    case BGE:
    case BGEU:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        set_q_v(reg_file, rob, inst.rs2_addr, &qk, &vk, dependencies, batch_size);
        // printf("vj: %d, qj: %d, vk: %d, qk: %d\n", vj, qj, vk, qk);
        break;
    default:
        fprintf(stderr, "Error: Unknown branch op");
        exit(EXIT_FAILURE);
        break;
    }

    // printf("BRANCH; ROB: %d; QJ: %d; VJ: %d; QK: %d; VK: %d\n", dest_rob_id, qj, vj, qk, vk);

    res_stations_add(
        branch_res_stations,
        inst.op,
        qj,
        qk,
        vj,
        vk,
        inst.imm,
        dest_rob_id,
        inst.inst_pc);
}

void handle_mem_operation(
    struct decoded_inst inst,
    struct rob *rob,
    struct reg_file *reg_file,
    struct memory_buffers *memory_buffers,
    struct dependency *dependencies,
    uint32_t batch_size)
{
    /*
        Store instructions have no rd_addr.
        But rd_addr should be NA from the decode unit for store instructions.
    */

    uint32_t dest_rob_id;

    uint32_t qj = NA;
    uint32_t qk = NA;
    uint32_t vj = NA;
    uint32_t vk = NA;

    switch (inst.op)
    {
    case LW:
    case LH:
    case LHU:
    case LB:
    case LBU:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        dest_rob_id = rob_enqueue(rob, inst.op_type, inst.rd_addr, NA, NA, NA, NA);
        reg_file_set_rob_id(reg_file, inst.rd_addr, dest_rob_id);
        add_dependency(dependencies, batch_size, inst.rd_addr, dest_rob_id);
        break;
    case SW:
    case SH:
    case SB:
        set_q_v(reg_file, rob, inst.rs1_addr, &qj, &vj, dependencies, batch_size);
        set_q_v(reg_file, rob, inst.rs2_addr, &qk, &vk, dependencies, batch_size);
        dest_rob_id = rob_enqueue(rob, inst.op_type, NA, vk, qk, NA, NA);
        break;
    default:
        fprintf(stderr, "Error: Unknown memory op");
        exit(EXIT_FAILURE);
        break;
    }

    // printf("MEMORY; ROB: %d; QJ: %d; VJ: %d; QK: %d; VK: %d\n", dest_rob_id, qj, vj, qk, vk);

    memory_buffers_enqueue(
        memory_buffers,
        inst.op,
        qj,
        qk,
        vj,
        vk,
        inst.imm,
        dest_rob_id);
}

uint32_t get_batch_size(struct issue_unit *issue_unit)
{
    uint32_t batch_size = min(inst_queue_cur_entries(issue_unit->inst_queue), rob_num_free_slots(issue_unit->rob));
    batch_size = min(batch_size, res_stations_num_free(issue_unit->alu_res_stations));
    batch_size = min(batch_size, res_stations_num_free(issue_unit->branch_res_stations));
    batch_size = min(batch_size, memory_buffers_num_free(issue_unit->memory_buffers));
    batch_size = min(batch_size, ISSUE_WIDTH);
    return batch_size;
}

void issue_step(struct issue_unit *issue_unit)
{
    uint32_t batch_size = get_batch_size(issue_unit);
    struct dependency dependencies[batch_size];
    for (uint32_t i = 0; i < batch_size; i++)
    {
        dependencies[i].busy = false;
    }

    // for (uint32_t i = 0; i < min(batch_size, 1); i++)
    for (uint32_t i = 0; i < batch_size; i++)
    {
        // printf("%d\n", i);
        struct decoded_inst inst = inst_queue_dequeue(issue_unit->inst_queue, i);
        switch (inst.op_type)
        {
        case AL:
            // printf("\tIssue: AL\n");
            handle_al_operation(inst, issue_unit->rob, issue_unit->reg_file, issue_unit->alu_res_stations, dependencies, batch_size);
            break;
        case JUMP:
        case BRANCH:
            // printf("\tIssue: JUMP/BRANCH\n");
            handle_branch_operation(inst, issue_unit->rob, issue_unit->reg_file, issue_unit->branch_res_stations, dependencies, batch_size);
            break;
        case LOAD:
        case STORE_WORD:
        case STORE_HALF:
        case STORE_BYTE:
            // printf("\tIssue: LOAD/STORE\n");
            handle_mem_operation(inst, issue_unit->rob, issue_unit->reg_file, issue_unit->memory_buffers, dependencies, batch_size);
            break;
        default:
            fprintf(stderr, "Error: Unknown instruction type");
            exit(EXIT_FAILURE);
            break;
        }
    }
}

void issue_destroy(struct issue_unit *issue_unit)
{
    free(issue_unit);
}
