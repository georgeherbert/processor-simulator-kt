# Compiler flags
CFLAGS = -Wall -Wextra -O3 -std=c11 $(OPTIONS)

# Source files
SRCS = \
	cpu.c \
	main_memory.c \
	reg_file.c \
	reg.c \
	fetch.c \
	decode.c \
	inst_queue.c \
	issue.c \
	res_stations.c \
	alu.c \
	branch.c \
	memory.c \
	cdb.c \
	rob.c \
	memory_buffers.c \
	address.c \
	commit.c \
	btb.c \

# Object files
OBJS = $(SRCS:.c=.o)

# Executable name
TARGET = cpu

.PHONY: all clean

# Default target
all: $(TARGET)

# Compile object files
%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

# Link object files into executable
$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(OBJS) -o $@

# Clean up
clean:
	rm -f $(OBJS) $(TARGET)