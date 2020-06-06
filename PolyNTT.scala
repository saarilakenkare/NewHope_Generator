package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class PolyNTT extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    //val constant_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val poly_s = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val gammas_bitrev_montgomery = VecInit(Array(
  4075.U,5315.U,7965.U,7373.U,522.U,10120.U,9027.U,5079.U,2344.U,1278.U,1973.U,5574.U,1018.U,6364.U,11248.U,8775.U,
  7500.U,7822.U,5537.U,4749.U,8500.U,12142.U,5456.U,7840.U,5445.U,3860.U,4536.U,11239.U,6171.U,8471.U,2683.U,11099.U,
  10561.U,400.U,6137.U,7341.U,5415.U,8646.U,6136.U,5862.U,5529.U,5206.U,56.U,9090.U,8724.U,11635.U,1702.U,10302.U,
  5339.U,6843.U,6093.U,3710.U,316.U,382.U,11821.U,8301.U,10930.U,5435.U,11035.U,973.U,8291.U,10256.U,8410.U,1922.U,
  12097.U,10968.U,10240.U,4912.U,4698.U,5057.U,7509.U,8844.U,8807.U,11502.U,5468.U,1010.U,9162.U,8120.U,2920.U,5241.U,
  6055.U,8953.U,677.U,5874.U,2766.U,10966.U,12237.U,9115.U,12138.U,10162.U,3957.U,2839.U,6383.U,2505.U,11858.U,1579.U,
  9026.U,3600.U,6077.U,4624.U,11868.U,4080.U,6068.U,3602.U,605.U,9987.U,504.U,8076.U,4782.U,6403.U,3029.U,6695.U,
  11184.U,142.U,5681.U,8812.U,2844.U,3438.U,8077.U,975.U,58.U,12048.U,1003.U,8757.U,885.U,6281.U,1956.U,5009.U,
  12225.U,3656.U,11606.U,9830.U,1566.U,5782.U,2503.U,2948.U,7032.U,3834.U,5919.U,4433.U,3054.U,6803.U,9166.U,1747.U,
  10211.U,11177.U,4322.U,1958.U,922.U,11848.U,4079.U,11231.U,4046.U,11580.U,1319.U,9139.U,6224.U,835.U,8049.U,8719.U,
  7105.U,1200.U,6122.U,9734.U,3956.U,1360.U,6119.U,5297.U,4298.U,3329.U,168.U,2692.U,1594.U,10327.U,5106.U,6328.U,
  3728.U,8240.U,5990.U,11130.U,948.U,1146.U,10885.U,325.U,8212.U,4016.U,8527.U,2919.U,295.U,6190.U,652.U,5766.U,
  11713.U,8326.U,6142.U,2447.U,1805.U,2882.U,10238.U,1954.U,1843.U,9928.U,4115.U,3030.U,2908.U,12071.U,8760.U,3434.U,
  5876.U,2281.U,2031.U,5333.U,8298.U,8320.U,12133.U,2767.U,11836.U,5908.U,11871.U,8517.U,6860.U,7515.U,10996.U,4737.U,
  2500.U,10800.U,5942.U,1583.U,11026.U,12240.U,5915.U,10806.U,1815.U,5383.U,1512.U,11939.U,2057.U,6920.U,9087.U,7796.U,
  8974.U,426.U,4754.U,1858.U,8532.U,10314.U,11942.U,2925.U,174.U,11566.U,3009.U,1693.U,2655.U,6554.U,5868.U,2738.U,
  11796.U,8193.U,9908.U,5444.U,10911.U,1912.U,7952.U,435.U,404.U,7644.U,11224.U,10146.U,7012.U,11121.U,11082.U,9041.U,
  9723.U,2187.U,9867.U,6250.U,3646.U,9852.U,6267.U,2987.U,8509.U,875.U,4976.U,10682.U,8005.U,5088.U,7278.U,11287.U,
  9223.U,27.U,3763.U,10849.U,11272.U,7404.U,5084.U,10657.U,8146.U,4714.U,12047.U,10752.U,2678.U,3704.U,545.U,7270.U,
  1067.U,5101.U,442.U,2401.U,390.U,11516.U,3778.U,8456.U,1045.U,9430.U,9808.U,5012.U,9377.U,6591.U,11935.U,4861.U,
  7852.U,3.U,3149.U,12129.U,12176.U,4919.U,10123.U,3915.U,3636.U,7351.U,2704.U,5291.U,1663.U,1777.U,1426.U,7635.U,
  1484.U,7394.U,2780.U,7094.U,8236.U,2645.U,7247.U,2305.U,2847.U,7875.U,7917.U,10115.U,10600.U,8925.U,4057.U,3271.U,
  9273.U,243.U,9289.U,11618.U,3136.U,5191.U,8889.U,9890.U,11869.U,5559.U,10111.U,10745.U,11813.U,8758.U,4905.U,3985.U,
  9603.U,9042.U,3978.U,9320.U,3510.U,5332.U,9424.U,2370.U,9405.U,11136.U,2249.U,8241.U,10659.U,10163.U,9103.U,6882.U,
  10810.U,1.U,5146.U,4043.U,8155.U,5736.U,11567.U,1305.U,1212.U,10643.U,9094.U,5860.U,8747.U,8785.U,8668.U,2545.U,
  4591.U,6561.U,5023.U,6461.U,10938.U,4978.U,6512.U,8961.U,949.U,2625.U,2639.U,7468.U,11726.U,2975.U,9545.U,9283.U,
  3091.U,81.U,11289.U,7969.U,9238.U,9923.U,2963.U,7393.U,12149.U,1853.U,11563.U,7678.U,8034.U,11112.U,1635.U,9521.U,
  3201.U,3014.U,1326.U,7203.U,1170.U,9970.U,11334.U,790.U,3135.U,3712.U,4846.U,2747.U,3553.U,7484.U,11227.U,2294.U,
  11267.U,9.U,9447.U,11809.U,11950.U,2468.U,5791.U,11745.U,10908.U,9764.U,8112.U,3584.U,4989.U,5331.U,4278.U,10616.U,
  4452.U,9893.U,8340.U,8993.U,130.U,7935.U,9452.U,6915.U,8541.U,11336.U,11462.U,5767.U,7222.U,2197.U,12171.U,9813.U,
  3241.U,729.U,3289.U,10276.U,9408.U,3284.U,2089.U,5092.U,11029.U,4388.U,5755.U,7657.U,10861.U,1696.U,2426.U,11955.U,
  4231.U,2548.U,11934.U,3382.U,10530.U,3707.U,3694.U,7110.U,3637.U,8830.U,6747.U,145.U,7399.U,5911.U,2731.U,8357.U,
  ))


  val do_algo = RegInit(false.B)
  val multiply_polys = RegInit(false.B)
  val ntt = RegInit(false.B)
  val output_correct = RegInit(false.B)
  

  val MultiplyPolysModule = MultiplyPolys()
  MultiplyPolysModule.io.start := multiply_polys
  MultiplyPolysModule.io.poly_in := poly_s
  MultiplyPolysModule.io.factors_in := gammas_bitrev_montgomery
  
  val NTTModule = NTT()
  NTTModule.io.start := ntt
  NTTModule.io.poly_in := poly_s
  NTTModule.io.powers_in := gammas_bitrev_montgomery
  
  when (io.start && !do_algo) {
    do_algo := true.B
    poly_s := io.poly_in
    multiply_polys := true.B
    // gammas_bitrev_montgomery := io.constant_in
    withClockAndReset(clock, reset) {
      printf("PolyNTT poly input: 0x%x\n", Cat(io.poly_in))
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

    when (MultiplyPolysModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("multiply polys message: 0x%x\n", Cat(MultiplyPolysModule.io.poly_out))
      }
      multiply_polys := false.B
      ntt := true.B
      poly_s := MultiplyPolysModule.io.poly_out
    }

    when (NTTModule.io.output_valid) {
      // poly_a := GenAModule.io.state_out
      withClockAndReset(clock, reset) {
        printf("ntt message: 0x%x\n", Cat(NTTModule.io.poly_out))
      }
      poly_s := NTTModule.io.poly_out
      output_correct := true.B
      ntt := false.B
    }

  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("PolyNTT Done: 0x%x\n", Cat(poly_s))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_s
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object PolyNTT {
  def apply(): PolyNTT = Module(new PolyNTT())
}
