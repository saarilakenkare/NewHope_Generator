package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class StatePermute extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val state_in = Input(UInt(1600.W))
    val state_out = Output(UInt(1600.W))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val state = RegInit(VecInit(Seq.fill(200)(0.U(8.W))))

  val do_algo = RegInit(false.B)
  val matrix = RegInit(VecInit(Seq.fill(25)(0.U(64.W))))
  val round = RegInit(0.U(8.W))
  val init = RegInit(false.B)
  val R = RegInit(1.U(8.W))

  val do_init_state = RegInit(false.B)
  val do_init_matrix = RegInit(false.B)
  val do_rounds = RegInit(false.B)
  val do_dstep = RegInit(false.B)
  val do_xystep = RegInit(false.B)
  val do_xstep = RegInit(false.B)
  val do_rstep = RegInit(false.B)
  val do_load_out = RegInit(false.B)

  val input = RegInit(0.U(1600.W))

  when (io.start && !do_algo) {
    withClockAndReset(clock, reset) {
      printf("StatePermute started: 0x%x\n", io.state_in)
    }
    do_load_out := false.B
    do_algo := true.B
    do_init_state := true.B
    input := io.state_in
    round := 0.U
    output_index := 0.U
    R := 1.U
  }
  when (do_algo) {
    output_reg := 0.U
    val i = RegInit(0.U(8.W))
    withClockAndReset(clock, reset) {
      printf("StatePermute do_algo: 0x%x\n", input)
    }
    when (do_init_state) {
      withClockAndReset(clock, reset) {
        //printf("init state\n")
      }
      for (i <- 0 until 200) {
        state(i) := input >> ((199 - i) * 8)
      }
      do_init_state := false.B
      do_init_matrix := true.B
    }
    when (do_init_matrix) {
      withClockAndReset(clock, reset) {
        //printf("init matrix\n")
      }
      for (idx <- 0 until 5) {
        for (jdx <- 0 until 5) {
            matrix(idx * 5 + jdx) := Cat(state(8 * (idx + 5 * jdx) + 7), state(8 * (idx + 5 * jdx) + 6), state(8 * (idx + 5 * jdx) + 5), state(8 * (idx + 5 * jdx) + 4), state(8 * (idx + 5 * jdx) + 3), state(8 * (idx + 5 * jdx) + 2), state(8 *(idx + 5 * jdx) + 1), state(8 * (idx + 5 * jdx) + 0))
        }
      }
      do_init_matrix := false.B
      do_rounds := true.B
      do_dstep := true.B
    }
    when (do_rounds) {
      withClockAndReset(clock, reset) {
        printf("doing rounds: %d\n", round)
        printf("do_dstep: %b\n", do_dstep)
        printf("do_xystep: %b\n", do_xystep)
        printf("do_xstep: %b\n", do_xstep)
        printf("do_rstep: %b\n", do_rstep)
      }
      when (do_dstep) {
        val c = VecInit(Seq.fill(5)(0.U(64.W)))
        for (idx <- 0 until 5) {
          c(idx) := matrix(idx*5) ^ matrix(idx*5+1) ^ matrix(idx*5+2) ^ matrix(idx*5+3) ^ matrix(idx*5+4)
        }
        val rol = VecInit(Seq.fill(5)(0.U(64.W)))
        for (idx <- 0 until 5) {
          rol(idx) := ((c(idx) << 1) ^ (c(idx) >> 63))
        }
        val d = VecInit(Seq.fill(5)(0.U(64.W)))
        for (idx <- 0 until 5) {
          d(idx) := c((idx + 4) % 5) ^ rol((idx + 1) % 5)
        }
        for (idx <- 0 until 5) {
          for (jdx <- 0 until 5) {
            matrix(idx * 5 + jdx) := matrix(idx * 5 + jdx) ^ d(idx)
          }
        }
        do_dstep := false.B
        do_xystep := true.B
        withClockAndReset(clock, reset) {
          printf("matrix before dstep: 0x%x\n", Cat(matrix))
        }
      }

      when (do_xystep) {
        var x = 1
        var y = 0
        var current = matrix(5)
        for(t <- 0 until 24) {
          val temp = y
          y = (2 * x + 3 * y) % 5
          x = temp
          val temp_cur = current
          current = matrix(5 * x + y)
          val mul = (t + 1) * (t + 2) / 2
          val left = (temp_cur << (mul % 64))
          val right = ((temp_cur >> (64 - (mul % 64))) & "hFFFFFFFFFFFFFFFF".U) 
          val new_val = left ^ right
            
          matrix(5 * x + y) := new_val
        }
        do_xystep := false.B
        do_xstep := true.B
        withClockAndReset(clock, reset) {
          printf("matrix before xystep: 0x%x\n", Cat(matrix))
        }
      }

      when (do_xstep) {
        for (j <- 0 until 5) {
          val x = VecInit(Seq.fill(5)(0.U(64.W)))
          for (i <- 0 until 5) {
            x(i) := matrix(5 * i + j)
          }
          for (i <- 0 until 5) {
            matrix(5 * i + j) := x(i) ^ ((~x((i + 1) % 5)) & (x((i + 2) % 5)))
          }
        }
        do_xstep := false.B
        do_rstep := true.B
        withClockAndReset(clock, reset) {
          printf("matrix before xstep: 0x%x\n", Cat(matrix))
        }
      }

      val rstep_i = RegInit(0.U(4.W))
      when (do_rstep) {
        R := ((R << 1) ^ ((R >> 7) * "h71".U)) % 256.U
        val R_wire = Wire(UInt(8.W))
        R_wire := ((R << 1) ^ ((R >> 7) * "h71".U)) % 256.U
        when ((R_wire & 2.U) === 2.U) {
          val pos = Wire(UInt(64.W))
          pos := 1.U << ((1.U << (rstep_i)) - 1.U)
          matrix(0) := matrix(0) ^ pos
        }
        rstep_i := rstep_i + 1.U
        when (rstep_i === 6.U) {
          do_rstep := false.B
          rstep_i := 0.U
        }
      }

      when (!do_dstep && !do_xystep && !do_xstep && !do_rstep) {
        withClockAndReset(clock, reset) {
          printf("matrix after round %d: 0x%x\n", round, Cat(matrix))
        }
        round := round + 1.U
        when (round === 23.U) {
          withClockAndReset(clock, reset) {
            printf("finished with rounds, loading state\n")
          }
          do_rounds := false.B
          do_load_out := true.B
          for (idx <- 0 until 5) {
            for (jdx <- 0 until 5) {
              for (kdx <- 0 until 8) {
                state(8*(idx+5*jdx)+kdx) := (matrix(5 * idx + jdx) >> (8 * kdx)) & "hFF".U
              }
            }
          }
        }
        .otherwise {
          do_dstep := true.B
        }
      }
    }
    when (do_load_out) {
      withClockAndReset(clock, reset) {
        printf("do_load_out\n")
      }
      when (!output_correct) {
        output_reg := (output_reg << 8) | state(output_index)
        output_index := output_index + 1.U
      }
      when (output_index === (199.U)) {
        output_correct := true.B
        withClockAndReset(clock, reset) {
          printf("output is finalized: 0x%x\n", output_reg)
        }
      }
    }
  }

  when (!output_correct) {
    io.state_out := io.state_in
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("StatePermute Sending: 0x%x\n", output_reg)
    }
    input := 0.U
    do_algo := false.B
    io.state_out := output_reg
    io.output_valid := true.B
    output_correct := false.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object StatePermute {
  def apply(): StatePermute = Module(new StatePermute())
}
