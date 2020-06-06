package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class NTT extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val powers_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val output_correct = RegInit(false.B)
  val output_index = RegInit(0.U(8.W))
  val output_reg = RegInit(0.U(1600.W))

  val QINV = 12287
  val Q = 12289

  val poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val powers_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))

  val do_algo = RegInit(false.B)
  
  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_loop = RegInit(false.B)
  val do_loop_init = RegInit(false.B)
  val do_loop_inner = RegInit(false.B)
  val do_loop_inner_init = RegInit(false.B)
  val do_loop_inner_2 = RegInit(false.B)
  val do_loop_inner_init_2 = RegInit(false.B)

  val index = RegInit(0.U(4.W))
  val start = RegInit(0.U(32.W))
  val j = RegInit(0.U(32.W))
  val distance = RegInit(1.U(32.W))
  val jTwiddle = RegInit(0.U(32.W))
  
  when (io.start && !do_algo) {
    do_algo := true.B
    do_loop := true.B
    do_loop_init := true.B
    poly_in := io.poly_in
    powers_in := io.powers_in
    output_reg := 0.U
    index := 0.U
    start := 0.U
    j := 0.U
    distance := 1.U
    jTwiddle := 0.U
    for (i <- 0 until 512) {
      poly_out(i) := io.poly_in(i)
    }
    withClockAndReset(clock, reset) {
      printf("NTT poly input: 0x%x\n", Cat(io.poly_in))
      printf("NTT powers input: 0x%x\n", Cat(io.powers_in))
    }
  }
  when (do_algo) {
    withClockAndReset(clock, reset) {
      //printf("ext seed: 0x%x\n", Cat(ext_seed))
      //printf("do_initstep: %b\n", do_initstep)
      //printf("do_aloop: %b\n", do_aloop)
      //printf("do_aloop_init: %b\n", do_aloop_init)
      //printf("do_aloop_for_s: %b\n", do_aloop_for_s)
      //printf("do_aloop_while_ctr: %b\n", do_aloop_while_ctr)
    }

    when (do_loop) {
      
      when (do_loop_init) {
        withClockAndReset(clock, reset) {
          // printf("do_loop_init\n")
        }
        distance := 1.U << index
        do_loop_init := false.B
        do_loop_inner_init := true.B
        start := 0.U
        withClockAndReset(clock, reset) {
          //printf("s: 0x%x\n", Cat(s))
          //printf("t: 0x%x\n", Cat(t))
        }
      }

      when (do_loop_inner_init) {
        withClockAndReset(clock, reset) {
          // printf("do_loop_inner_init\n")
        }
        jTwiddle := 0.U
        do_loop_inner_init := false.B
        do_loop_inner := true.B
        j := start
      }

      when (do_loop_inner) {
        val W = powers_in(jTwiddle)
        val temp = poly_out(j)
        withClockAndReset(clock, reset) {
          printf("j: %d, temp: 0x%x, poly_out(j+distance): 0x%x\n", j, temp, poly_out(j+distance))
        }
        poly_out(j) := temp + poly_out(j + distance)
         
        val mul = W * (temp + 3.U*Q.U - poly_out(j + distance))
        
        val u = Wire(UInt(32.W))
        val r = Wire(UInt(32.W))
        val v = Wire(UInt(32.W))
        val w = Wire(UInt(32.W))
        val s = Wire(UInt(32.W))
        val mont_reduction = Wire(UInt(32.W))

        u := mul * QINV.U
        r := 1.U << 18
        v := u & (r - 1.U)
        w := v * Q.U
        s := mul + w
        mont_reduction := s >> 18
        poly_out(j + distance) := mont_reduction
        
        jTwiddle := jTwiddle + 1.U
        j := j + 2.U * distance
        when ((j + (2.U * distance)) >= 511.U) {
          // do_loop_inner := false.B
          do_loop_inner := false.B
          // output_correct := true.B
          when (start === (distance - 1.U)) {
            // output_correct := true.B
            when (index < 8.U) {
              start := 0.U
              distance := distance << 1
              do_loop_inner_init_2 := true.B
            }
            .otherwise {
              // output_correct := true.B
              // when (index === 8.U) {
                output_correct := true.B
              // }
              // .otherwise {
              //  index := index + 2.U
              //}
            }
          }
          .otherwise {
            start := start + 1.U
            do_loop_inner_init := true.B
          }
        }

        withClockAndReset(clock, reset) {
          printf("setting ntt_poly[%d]: 0x%x\n", j, temp + poly_out(j + distance))
          printf("setting ntt_poly[%d]: 0x%x\n", j + distance, mont_reduction)
        }
      }
        
      when (do_loop_inner_init_2) {
        withClockAndReset(clock, reset) {
          printf("starting inner loop 2\n")
        }
        jTwiddle := 0.U
        do_loop_inner_init_2 := false.B
        do_loop_inner_2 := true.B
        j := start
      }

      when (do_loop_inner_2) {
        val W = powers_in(jTwiddle)
        val temp = poly_out(j)
        withClockAndReset(clock, reset) {
          printf("j: %d, temp: 0x%x, poly_out(j+distance): 0x%x\n", j, temp, poly_out(j+distance))
        }
        poly_out(j) := (temp + poly_out(j + distance)) % Q.U
         
        val mul = W * (temp + 3.U*Q.U - poly_out(j + distance))
        
        val u = Wire(UInt(32.W))
        val r = Wire(UInt(32.W))
        val v = Wire(UInt(32.W))
        val w = Wire(UInt(32.W))
        val s = Wire(UInt(32.W))
        val mont_reduction = Wire(UInt(32.W))

        u := mul * QINV.U
        r := 1.U << 18
        v := u & (r - 1.U)
        w := v * Q.U
        s := mul + w
        mont_reduction := s >> 18
        poly_out(j + distance) := mont_reduction
        
        jTwiddle := jTwiddle + 1.U
        j := j + 2.U * distance
        when ((j + (2.U * distance)) >= 511.U) {
          // do_loop_inner := false.B
          do_loop_inner_2 := false.B
          // output_correct := true.B
          when (start >= (distance - 1.U)) {
            // output_correct := true.B
            index := index + 2.U
            do_loop_init := true.B
          }
          .otherwise {
            start := start + 1.U
            do_loop_inner_init_2 := true.B
          }
        }

        withClockAndReset(clock, reset) {
          printf("setting ntt_poly[%d]: 0x%x\n", j, temp + poly_out(j + distance))
          printf("setting ntt_poly[%d]: 0x%x\n", j + distance, mont_reduction)
        }
      }

    }
  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("NTT Done: 0x%x\n", Cat(poly_out))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_out
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object NTT {
  def apply(): NTT = Module(new NTT())
}
