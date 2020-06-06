package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class Shake256 extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val state_in = Input(UInt())
    val length_in = Input(UInt(8.W))
    val length_out = Input(UInt(8.W))
    val state_out = Output(UInt())
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val d = "h1F".U
  val rate = 136.U

  val state = RegInit(VecInit(Seq.fill(200)(0.U(8.W))))
  val input = RegInit(0.U)
  val length_in = RegInit(0.U(8.W))
  val length_out = RegInit(0.U(8.W))

  val block_size = RegInit(0.U(16.W))
  val input_offset = RegInit(0.U(16.W))

  val do_algo = RegInit(false.B)
  val matrix = RegInit(VecInit(Seq.fill(25)(0.U(64.W))))
  val round = RegInit(0.U(8.W))
  val init = RegInit(false.B)
  val R = RegInit(1.U(8.W))

  val Aba = Wire(UInt(64.W))
  val Abe = Wire(UInt(64.W))
  val Abi = Wire(UInt(64.W))
  val Abo = Wire(UInt(64.W))
  val Abu = Wire(UInt(64.W))

  val Aga = Wire(UInt(64.W))
  val Age = Wire(UInt(64.W))
  val Agi = Wire(UInt(64.W))
  val Ago = Wire(UInt(64.W))
  val Agu = Wire(UInt(64.W))

  val Aka = Wire(UInt(64.W))
  val Ake = Wire(UInt(64.W))
  val Aki = Wire(UInt(64.W))
  val Ako = Wire(UInt(64.W))
  val Aku = Wire(UInt(64.W))
  
  val Ama = Wire(UInt(64.W))
  val Ame = Wire(UInt(64.W))
  val Ami = Wire(UInt(64.W))
  val Amo = Wire(UInt(64.W))
  val Amu = Wire(UInt(64.W))

  val Asa = Wire(UInt(64.W))
  val Ase = Wire(UInt(64.W))
  val Asi = Wire(UInt(64.W))
  val Aso = Wire(UInt(64.W))
  val Asu = Wire(UInt(64.W))

  Aba := 0.U
  Abe := 0.U
  Abi := 0.U
  Abo := 0.U
  Abu := 0.U

  Aga := 0.U
  Age := 0.U
  Agi := 0.U
  Ago := 0.U
  Agu := 0.U

  Aka := 0.U
  Ake := 0.U
  Aki := 0.U
  Ako := 0.U
  Aku := 0.U

  Ama := 0.U
  Ame := 0.U
  Ami := 0.U
  Amo := 0.U
  Amu := 0.U

  Asa := 0.U
  Ase := 0.U
  Asi := 0.U
  Aso := 0.U
  Asu := 0.U

  val do_rounds = RegInit(false.B)
  val do_dstep = RegInit(false.B)
  val do_xystep = RegInit(false.B)
  val do_xstep = RegInit(false.B)
  val do_rstep = RegInit(false.B)
  val do_loadstep = RegInit(false.B)

  val i = RegInit(0.U(8.W))
  
  when (io.start && !do_algo) {
    i := 0.U
    do_algo := true.B
    output_index := 0.U
    output_reg := 0.U
    input := io.state_in
    length_in := io.length_in
    length_out := io.length_out
    block_size := 0.U
    input_offset := 0.U
    init := false.B
    state := VecInit(Seq.fill(200)(0.U(8.W)))
    withClockAndReset(clock, reset) {
      printf("Shake256 input: 0x%x\n", io.state_in) 
    }
  }
  when (do_algo) {
    output_reg := 0.U
    withClockAndReset(clock, reset) {
      // printf("Shake256 input: 0x%x\n", io.state_in)
      // printf("Shake256 input offset: 0x%x\n", input_offset)
      // printf("Shake256 length in: 0x%x\n", io.length_in)
      //printf("Shake256 state[0]: 0x%x\n", state(0))
      //printf("Shake256 state[1]: 0x%x\n", state(1))
      //printf("Shake256 state[2]: 0x%x\n", state(2))
      // printf("Shake 256 state: 0x")
      for (idx <- 0 until 200) {
      //  printf("%x", state(idx))
      }
      //printf("\n")

      // printf("do_dstep: %b\n", do_dstep)
      // printf("do_xystep: %b\n", do_xystep)
      // printf("do_xstep: %b\n", do_xstep)
      // printf("do_rstep: %b\n", do_rstep)
    }
    when (input_offset < length_in) {
      withClockAndReset(clock, reset) {
        // printf("shake256 first when triggered, input is 0x%x\n", input)
      }
      block_size := rate
      when ((length_in - input_offset) < rate) {
        block_size := length_in - input_offset
      }
      withClockAndReset(clock, reset) {
        //printf("block_size: %d\n", block_size)
      }
      val in_i = (input >> (8.U * (length_in - i - 1.U))) & "hFF".U
      withClockAndReset(clock, reset) {
        //printf("i: 0x%x\n", i)
        //printf("in_i: 0x%x\n", in_i)
      }
      when (i < block_size) {
        state(i) := state(i) ^ in_i
        i := i + 1.U
      }
      .otherwise {
        input_offset := input_offset + block_size
      }
      // state(i) := state(i) ^ io.state_in(i + input_offset, i + input_offset + 1.U)
    }
    .elsewhen (i === block_size) {
      withClockAndReset(clock, reset) {
        // printf("shake256 elsewhen triggered, input is 0x%x\n", input)
        // printf("shake256 state: 0x%x\n", Cat(state))
      }
      state(block_size) := state(block_size) ^ d
      state(rate - 1.U) := state(rate - 1.U) ^ "h80".U
      i := i + 1.U
    }
    .otherwise {
      withClockAndReset(clock, reset) {
        //printf("otherwise\n")
      }
      // matrix(0) := Cat(state(7), state(6), state(5), state(4), state(3), state(2), state(1), state(0))
      withClockAndReset(clock, reset) {
        //printf("round: %d\n", round)
        //printf("init: %b\n", init)
      }
      for (idx <- 0 until 5) {
        for (jdx <- 0 until 5) {
            withClockAndReset(clock, reset) {
              //printf("idx: %d\n", (idx * 5 + jdx).U)
              //printf("value: 0x%x\n", matrix(idx * 5 + jdx))
            }
        }
      }
      when (init === false.B) {
        withClockAndReset(clock, reset) {
          //printf("initializing\n")
          //printf("state(32): 0x%x\n", state(32))
        }
        for (idx <- 0 until 5) {
          for (jdx <- 0 until 5) {
            // val value = RegInit(0.U(64.W))
            matrix(idx * 5 + jdx) := Cat(state(8 * (idx + 5 * jdx) + 7), state(8 * (idx + 5 * jdx) + 6), state(8 * (idx + 5 * jdx) + 5), state(8 * (idx + 5 * jdx) + 4), state(8 * (idx + 5 * jdx) + 3), state(8 * (idx + 5 * jdx) + 2), state(8 *(idx + 5 * jdx) + 1), state(8 * (idx + 5 * jdx) + 0))
            // matrix(idx * 5 + jdx) := value
          }
        }
        Aba := matrix(0)
        Abe := matrix(1)
        Abi := matrix(2)
        Abo := matrix(3)
        Abu := matrix(4)

        Aga := matrix(5)
        Age := matrix(6)
        Agi := matrix(7)
        Ago := matrix(8)
        Agu := matrix(9)
        
        Aka := matrix(9)
        Ake := matrix(11)
        Aki := matrix(12)
        Ako := matrix(13)
        Aku := matrix(14)

        Ama := matrix(15)
        Ame := matrix(16)
        Ami := matrix(17)
        Amo := matrix(18)
        Amu := matrix(19)

        Asa := matrix(20)
        Ase := matrix(21)
        Asi := matrix(22)
        Aso := matrix(23)
        Asu := matrix(24)
        init := true.B
        do_rounds := true.B
        do_dstep := true.B
      } 
      .elsewhen (do_rounds) {
        withClockAndReset(clock, reset) {
          //printf("do_rounds\n")
          //printf("SPM io.state_in: 0x%x\n", Cat(state))
        }
        val StatePermuteModule = StatePermute()
        StatePermuteModule.io.start := do_rounds
        StatePermuteModule.io.state_in := Cat(state)
        when (StatePermuteModule.io.output_valid) {
          withClockAndReset(clock, reset) {
            printf("StatePermuteModule out: 0x%x\n", StatePermuteModule.io.state_out)
          }
          do_rounds := false.B
          do_loadstep := true.B
          val spm_out = StatePermuteModule.io.state_out
          for (i <- 0 until 200) {
            state(i) := spm_out >> (8 * (199 - i))
          }
        }
      }

        /*withClockAndReset(clock, reset) {
          printf("initialized\n")
        }

        // val BCa = Aba ^ Aga ^ Aka ^ Ama ^ Asa;
        // val BCe = Abe ^ Age ^ Ake ^ Ame ^ Ase;
        // val BCi = Abi ^ Agi ^ Aki ^ Ami ^ Asi;
        // val BCo = Abo ^ Ago ^ Ako ^ Amo ^ Aso;
        // val BCu = Abu ^ Agu ^ Aku ^ Amu ^ Asu;

        // val BCa_ROL1 = (BCa << 1) ^ (BCa >> 63)
        // val BCe_ROL1 = (BCe << 1) ^ (BCe >> 63)
        // val BCi_ROL1 = (BCi << 1) ^ (BCi >> 63)
        // val BCo_ROL1 = (BCo << 1) ^ (BCo >> 63)
        // val BCu_ROL1 = (BCu << 1) ^ (BCu >> 63)
        
        // val Da = BCu ^ BCe_ROL1
        // val De = BCa ^ BCi_ROL1
        // val Di = BCe ^ BCo_ROL1
        // val Do = BCi ^ BCu_ROL1
        // val Du = BCo ^ BCa_ROL1

        // Aba := Aba ^ Da
        // BCa = Aba
        //

        when (do_dstep) {
          val c = Vec(Seq.fill(5)(0.U(64.W)))
          for (idx <- 0 until 5) {
            c(idx) := matrix(idx*5) ^ matrix(idx*5+1) ^ matrix(idx*5+2) ^ matrix(idx*5+3) ^ matrix(idx*5+4)
          }
          val rol = Vec(Seq.fill(5)(0.U(64.W)))
          for (idx <- 0 until 5) {
            rol(idx) := ((c(idx) << 1) ^ (c(idx) >> 63))
          }
          val d = Vec(Seq.fill(5)(0.U(64.W)))
          for (idx <- 0 until 5) {
            d(idx) := c((idx + 4) % 5) ^ rol((idx + 1) % 5)
          }
          //val d_matrix = Vec(Seq.fill(25)(0.U(64.W)))
          for (idx <- 0 until 5) {
            for (jdx <- 0 until 5) {
              matrix(idx * 5 + jdx) := matrix(idx * 5 + jdx) ^ d(idx)
            }
          }
          do_dstep := false.B
          do_xystep := true.B
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
            
            withClockAndReset(clock, reset) {
              // printf("temp_cur: 0x%x\n", temp_cur)
              // printf("mul: %d\n", mul.U)
              // printf("left: 0x%x, right: 0x%x\n", left, right)
              // printf("setting matrix[%d][%d]: 0x%x\n", x.U, y.U, new_val)
            }
            matrix(5 * x + y) := new_val
          }
        //  for (idx <- 0 until 5) {
        //    for (jdx <- 0 until 5) {
        //      matrix(idx * 5 + jdx) := d_matrix(idx * 5 + jdx)
        //  }
        // }
          do_xystep := false.B
          do_xstep := true.B
        }

        when (do_xstep) {
          for (j <- 0 until 5) {
            val x = Vec(Seq.fill(5)(0.U(64.W)))
            for (i <- 0 until 5) {
              x(i) := matrix(5 * i + j)
            }
            for (i <- 0 until 5) {
              matrix(5 * i + j) := x(i) ^ ((~x((i + 1) % 5)) & (x((i + 2) % 5)))
            }
          }
          do_xstep := false.B
          do_rstep := true.B
        }

        val rstep_i = RegInit(0.U(4.W))
        when (do_rstep) {
          // val R_wire = Wire(UInt(8.W))
          // R_wire := R
          // for (i <- 0 until 7) {
            R := ((R << 1) ^ ((R >> 7) * "h71".U)) % 256.U
            val R_wire = Wire(UInt(8.W))
            R_wire := ((R << 1) ^ ((R >> 7) * "h71".U)) % 256.U
            withClockAndReset(clock, reset) {
              // printf("R: %d\n", R_wire)
              // printf("rstep_i: %d\n", rstep_i)
            }
            when ((R_wire & 2.U) === 2.U) {
              // withClockAndReset(clock, reset) {
              //  printf("R: %d\n", R_wire)
              //  printf("rstep_i: %d\n", rstep_i)
              //}
              val pos = Wire(UInt(64.W))
              pos := 1.U << ((1.U << (rstep_i)) - 1.U)
              withClockAndReset(clock, reset) {
                // printf("R triggered this: 0x%x\n", R_wire)
                // printf("rstep_i: 0x%x\n", rstep_i)
                // printf("pos: 0x%x\n", pos)
              }
              // val base = 1 << pos
              matrix(0) := matrix(0) ^ pos
            }
          // }
          // R := R_wire
          rstep_i := rstep_i + 1.U
          when (rstep_i === 6.U) {
            do_rstep := false.B
            rstep_i := 0.U
          }
        }

        when (!do_dstep && !do_xystep && !do_xstep && !do_rstep) {
          round := round + 1.U
          when (round === 23.U) {
            withClockAndReset(clock, reset) {
              printf("finished with rounds\n")
            }
            do_rounds := false.B
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
        }*/

      when(do_loadstep) {
        withClockAndReset(clock, reset) {
          //printf("state is finalized: 0x%x\n", output_reg)
          for (i <- 0 until 200) {
            // printf("%x", state(i)) 
          }
          //printf("\n")
          
        }
        when (!output_correct) {
          output_reg := (output_reg << 8) | state(output_index)
          output_index := output_index + 1.U
        }
        when (output_index === (length_out - 1.U)) {
          do_loadstep := false.B
          output_correct := true.B
          withClockAndReset(clock, reset) {
            printf("shake256 output is finalized: 0x%x\n", output_reg)
            printf("for input 0x%x\n", input)
          }
          // io.state_out := output_reg
          // io.output_valid := true.B
        }
      }
    }
  }

  when (!output_correct) {
    io.state_out := io.state_in
    io.output_valid := false.B
  }
  .otherwise {
    do_algo := false.B
    output_correct := false.B
    withClockAndReset(clock, reset) {
      printf("Shake256 Sending: 0x%x\n", output_reg)
    }
    io.state_out := output_reg
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object Shake256 {
  def apply(): Shake256 = Module(new Shake256())
}
