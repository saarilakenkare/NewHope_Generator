package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class PolyInvNTT extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    //val constant_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val poly_out = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))

  val do_algo = RegInit(false.B)
  val reverse_poly = RegInit(false.B)
  val multiply_polys = RegInit(false.B)
  val ntt = RegInit(false.B)
  val output_correct = RegInit(false.B)
  

  val omegas_inv_bitrev_montgomery = VecInit(Array(
  4075.U,6974.U,4916.U,4324.U,7210.U,3262.U,2169.U,11767.U,3514.U,1041.U,5925.U,11271.U,6715.U,10316.U,11011.U,9945.U,
  1190.U,9606.U,3818.U,6118.U,1050.U,7753.U,8429.U,6844.U,4449.U,6833.U,147.U,3789.U,7540.U,6752.U,4467.U,4789.U,
  10367.U,3879.U,2033.U,3998.U,11316.U,1254.U,6854.U,1359.U,3988.U,468.U,11907.U,11973.U,8579.U,6196.U,5446.U,6950.U,
  1987.U,10587.U,654.U,3565.U,3199.U,12233.U,7083.U,6760.U,6427.U,6153.U,3643.U,6874.U,4948.U,6152.U,11889.U,1728.U,
  7280.U,10333.U,6008.U,11404.U,3532.U,11286.U,241.U,12231.U,11314.U,4212.U,8851.U,9445.U,3477.U,6608.U,12147.U,1105.U,
  5594.U,9260.U,5886.U,7507.U,4213.U,11785.U,2302.U,11684.U,8687.U,6221.U,8209.U,421.U,7665.U,6212.U,8689.U,3263.U,
  10710.U,431.U,9784.U,5906.U,9450.U,8332.U,2127.U,151.U,3174.U,52.U,1323.U,9523.U,6415.U,11612.U,3336.U,6234.U,
  7048.U,9369.U,4169.U,3127.U,11279.U,6821.U,787.U,3482.U,3445.U,4780.U,7232.U,7591.U,7377.U,2049.U,1321.U,192.U,
  9551.U,6421.U,5735.U,9634.U,10596.U,9280.U,723.U,12115.U,9364.U,347.U,1975.U,3757.U,10431.U,7535.U,11863.U,3315.U,
  4493.U,3202.U,5369.U,10232.U,350.U,10777.U,6906.U,10474.U,1483.U,6374.U,49.U,1263.U,10706.U,6347.U,1489.U,9789.U,
  7552.U,1293.U,4774.U,5429.U,3772.U,418.U,6381.U,453.U,9522.U,156.U,3969.U,3991.U,6956.U,10258.U,10008.U,6413.U,
  8855.U,3529.U,218.U,9381.U,9259.U,8174.U,2361.U,10446.U,10335.U,2051.U,9407.U,10484.U,9842.U,6147.U,3963.U,576.U,
  6523.U,11637.U,6099.U,11994.U,9370.U,3762.U,8273.U,4077.U,11964.U,1404.U,11143.U,11341.U,1159.U,6299.U,4049.U,8561.U,
  5961.U,7183.U,1962.U,10695.U,9597.U,12121.U,8960.U,7991.U,6992.U,6170.U,10929.U,8333.U,2555.U,6167.U,11089.U,5184.U,
  3570.U,4240.U,11454.U,6065.U,3150.U,10970.U,709.U,8243.U,1058.U,8210.U,441.U,11367.U,10331.U,7967.U,1112.U,2078.U,
  10542.U,3123.U,5486.U,9235.U,7856.U,6370.U,8455.U,5257.U,9341.U,9786.U,6507.U,10723.U,2459.U,683.U,8633.U,64.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U, 0.U,
  ))

  val gammas_inv_montgomery = VecInit(Array(
  512.U,3944.U,4267.U,5411.U,9615.U,5900.U,3205.U,6063.U,9261.U,2021.U,3087.U,4770.U,1029.U,1590.U,343.U,530.U,
  8307.U,4273.U,2769.U,9617.U,923.U,7302.U,4404.U,2434.U,1468.U,9004.U,8682.U,11194.U,2894.U,11924.U,5061.U,8071.U,
  1687.U,10883.U,8755.U,7724.U,11111.U,6671.U,7800.U,6320.U,2600.U,6203.U,4963.U,6164.U,9847.U,6151.U,11475.U,10243.U,
  3825.U,11607.U,1275.U,3869.U,425.U,5386.U,4238.U,9988.U,5509.U,11522.U,10029.U,7937.U,3343.U,6742.U,9307.U,10440.U,
  11295.U,3480.U,3765.U,1160.U,1255.U,4483.U,8611.U,9687.U,11063.U,3229.U,7784.U,9269.U,6691.U,7186.U,10423.U,10588.U,
  11667.U,11722.U,3889.U,12100.U,9489.U,12226.U,3163.U,12268.U,9247.U,12282.U,11275.U,4094.U,11951.U,5461.U,8080.U,10013.U,
  10886.U,7434.U,7725.U,2478.U,2575.U,826.U,9051.U,8468.U,3017.U,6919.U,5102.U,10499.U,5797.U,7596.U,10125.U,2532.U,
  3375.U,844.U,1125.U,8474.U,375.U,6921.U,125.U,2307.U,4138.U,769.U,9572.U,8449.U,7287.U,11009.U,2429.U,7766.U,
  4906.U,6685.U,9828.U,10421.U,3276.U,7570.U,1092.U,10716.U,364.U,3572.U,8314.U,5287.U,10964.U,9955.U,7751.U,11511.U,
  6680.U,3837.U,6323.U,1279.U,6204.U,8619.U,2068.U,2873.U,8882.U,5054.U,7057.U,5781.U,10545.U,1927.U,3515.U,8835.U,
  5268.U,2945.U,1756.U,5078.U,8778.U,5789.U,2926.U,6026.U,9168.U,6105.U,3056.U,2035.U,5115.U,8871.U,1705.U,2957.U,
  8761.U,5082.U,11113.U,1694.U,11897.U,4661.U,8062.U,5650.U,10880.U,10076.U,7723.U,7455.U,10767.U,2485.U,3589.U,9021.U,
  9389.U,3007.U,7226.U,9195.U,6505.U,3065.U,10361.U,5118.U,7550.U,1706.U,6613.U,4665.U,10397.U,1555.U,7562.U,8711.U,
  6617.U,7000.U,6302.U,10526.U,6197.U,7605.U,6162.U,2535.U,2054.U,845.U,4781.U,4378.U,5690.U,9652.U,5993.U,11410.U,
  6094.U,11996.U,10224.U,8095.U,3408.U,10891.U,1136.U,11823.U,4475.U,3941.U,5588.U,5410.U,5959.U,9996.U,10179.U,3332.U,
  3393.U,5207.U,1131.U,5832.U,377.U,1944.U,4222.U,648.U,9600.U,216.U,3200.U,72.U,5163.U,24.U,1721.U,8.U,
  4670.U,4099.U,5653.U,9559.U,10077.U,11379.U,3359.U,3793.U,5216.U,9457.U,5835.U,11345.U,1945.U,7878.U,8841.U,2626.U,
  2947.U,9068.U,9175.U,7119.U,11251.U,2373.U,11943.U,791.U,3981.U,4360.U,1327.U,9646.U,8635.U,11408.U,11071.U,7899.U,
  11883.U,2633.U,3961.U,4974.U,9513.U,1658.U,3171.U,4649.U,1057.U,5646.U,8545.U,1882.U,11041.U,8820.U,11873.U,2940.U,
  8054.U,980.U,6781.U,4423.U,10453.U,9667.U,11677.U,11415.U,12085.U,3805.U,12221.U,9461.U,8170.U,7250.U,10916.U,6513.U,
  7735.U,2171.U,10771.U,4820.U,11783.U,5703.U,8024.U,1901.U,6771.U,4730.U,2257.U,5673.U,8945.U,1891.U,7078.U,8823.U,
  10552.U,2941.U,11710.U,9173.U,12096.U,7154.U,4032.U,6481.U,1344.U,10353.U,448.U,3451.U,8342.U,9343.U,6877.U,11307.U,
  10485.U,3769.U,3495.U,9449.U,1165.U,7246.U,8581.U,10608.U,11053.U,3536.U,11877.U,5275.U,3959.U,9951.U,5416.U,3317.U,
  9998.U,5202.U,7429.U,1734.U,10669.U,578.U,11749.U,4289.U,12109.U,5526.U,12229.U,1842.U,12269.U,614.U,8186.U,4301.U,
  6825.U,5530.U,2275.U,10036.U,8951.U,11538.U,7080.U,3846.U,2360.U,1282.U,4883.U,8620.U,5724.U,11066.U,1908.U,7785.U,
  636.U,2595.U,212.U,865.U,4167.U,8481.U,1389.U,2827.U,463.U,9135.U,8347.U,3045.U,10975.U,1015.U,11851.U,8531.U,
  12143.U,6940.U,8144.U,10506.U,6811.U,3502.U,10463.U,9360.U,7584.U,3120.U,2528.U,1040.U,4939.U,4443.U,9839.U,1481.U,
  7376.U,4590.U,6555.U,1530.U,2185.U,510.U,8921.U,170.U,7070.U,4153.U,6453.U,9577.U,2151.U,11385.U,717.U,3795.U,
  239.U,1265.U,4176.U,4518.U,1392.U,1506.U,464.U,502.U,4251.U,8360.U,1417.U,6883.U,8665.U,10487.U,11081.U,7592.U,
  7790.U,6627.U,6693.U,2209.U,2231.U,8929.U,4840.U,11169.U,9806.U,3723.U,7365.U,1241.U,2455.U,4510.U,9011.U,9696.U,
  7100.U,3232.U,6463.U,9270.U,10347.U,3090.U,3449.U,1030.U,5246.U,8536.U,5845.U,11038.U,10141.U,11872.U,11573.U,12150.U,
  7954.U,4050.U,10844.U,1350.U,7711.U,450.U,10763.U,150.U,7684.U,50.U,10754.U,4113.U,7681.U,1371.U,10753.U,457.U,
  ))

  val ReversePolyModule = ReversePoly()
  ReversePolyModule.io.start := reverse_poly
  ReversePolyModule.io.poly_in := poly_in
  
  val NTTModule = NTT()
  NTTModule.io.start := ntt
  NTTModule.io.poly_in := poly_in
  NTTModule.io.powers_in := omegas_inv_bitrev_montgomery
  
  val MultiplyPolysModule = MultiplyPolys()
  MultiplyPolysModule.io.start := multiply_polys
  MultiplyPolysModule.io.poly_in := poly_in
  MultiplyPolysModule.io.factors_in := gammas_inv_montgomery
  
  when (io.start && !do_algo) {
    do_algo := true.B
    poly_in := io.poly_in
    // multiply_polys := true.B
    reverse_poly := true.B
    // gammas_bitrev_montgomery := io.constant_in
    withClockAndReset(clock, reset) {
      printf("PolyInvNTT poly input: 0x%x\n", Cat(io.poly_in))
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

    when (ReversePolyModule.io.output_valid) {
      // poly_a := GenAModule.io.state_out
      withClockAndReset(clock, reset) {
        printf("reverse message: 0x%x\n", Cat(ReversePolyModule.io.poly_out))
      }
      poly_in := ReversePolyModule.io.poly_out
      ntt := true.B
      reverse_poly := false.B
    }

    when (NTTModule.io.output_valid) {
      // poly_a := GenAModule.io.state_out
      withClockAndReset(clock, reset) {
        printf("ntt message: 0x%x\n", Cat(NTTModule.io.poly_out))
      }
      poly_in := NTTModule.io.poly_out
      ntt := false.B
      multiply_polys := true.B
    }

    when (MultiplyPolysModule.io.output_valid) {
      withClockAndReset(clock, reset) {
        printf("multiply polys message: 0x%x\n", Cat(MultiplyPolysModule.io.poly_out))
      }
      multiply_polys := false.B
      poly_out := MultiplyPolysModule.io.poly_out
      output_correct := true.B
    }

  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("PolyInvNTT Done: 0x%x\n", Cat(poly_out))
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

object PolyInvNTT {
  def apply(): PolyInvNTT = Module(new PolyInvNTT())
}
