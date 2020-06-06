package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class ReversePoly extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val poly_in = Input(Vec(512, UInt(16.W)))
    val poly_out = Output(Vec(512, UInt(16.W)))
    val output_valid = Output(Bool())
  })

  val bitrev_table = VecInit(Array(
  0.U,256.U,128.U,384.U,64.U,320.U,192.U,448.U,32.U,288.U,160.U,416.U,96.U,352.U,224.U,480.U,16.U,272.U,144.U,400.U,80.U,336.U,208.U,464.U,48.U,304.U,176.U,432.U,112.U,368.U,240.U,496.U,8.U,
  264.U,136.U,392.U,72.U,328.U,200.U,456.U,40.U,296.U,168.U,424.U,104.U,360.U,232.U,488.U,24.U,280.U,152.U,408.U,88.U,344.U,216.U,472.U,56.U,312.U,184.U,440.U,120.U,376.U,248.U,504.U,4.U,
  260.U,132.U,388.U,68.U,324.U,196.U,452.U,36.U,292.U,164.U,420.U,100.U,356.U,228.U,484.U,20.U,276.U,148.U,404.U,84.U,340.U,212.U,468.U,52.U,308.U,180.U,436.U,116.U,372.U,244.U,500.U,12.U,
  268.U,140.U,396.U,76.U,332.U,204.U,460.U,44.U,300.U,172.U,428.U,108.U,364.U,236.U,492.U,28.U,284.U,156.U,412.U,92.U,348.U,220.U,476.U,60.U,316.U,188.U,444.U,124.U,380.U,252.U,508.U,2.U,
  258.U,130.U,386.U,66.U,322.U,194.U,450.U,34.U,290.U,162.U,418.U,98.U,354.U,226.U,482.U,18.U,274.U,146.U,402.U,82.U,338.U,210.U,466.U,50.U,306.U,178.U,434.U,114.U,370.U,242.U,498.U,10.U,
  266.U,138.U,394.U,74.U,330.U,202.U,458.U,42.U,298.U,170.U,426.U,106.U,362.U,234.U,490.U,26.U,282.U,154.U,410.U,90.U,346.U,218.U,474.U,58.U,314.U,186.U,442.U,122.U,378.U,250.U,506.U,6.U,
  262.U,134.U,390.U,70.U,326.U,198.U,454.U,38.U,294.U,166.U,422.U,102.U,358.U,230.U,486.U,22.U,278.U,150.U,406.U,86.U,342.U,214.U,470.U,54.U,310.U,182.U,438.U,118.U,374.U,246.U,502.U,14.U,
  270.U,142.U,398.U,78.U,334.U,206.U,462.U,46.U,302.U,174.U,430.U,110.U,366.U,238.U,494.U,30.U,286.U,158.U,414.U,94.U,350.U,222.U,478.U,62.U,318.U,190.U,446.U,126.U,382.U,254.U,510.U,1.U,
  257.U,129.U,385.U,65.U,321.U,193.U,449.U,33.U,289.U,161.U,417.U,97.U,353.U,225.U,481.U,17.U,273.U,145.U,401.U,81.U,337.U,209.U,465.U,49.U,305.U,177.U,433.U,113.U,369.U,241.U,497.U,9.U,
  265.U,137.U,393.U,73.U,329.U,201.U,457.U,41.U,297.U,169.U,425.U,105.U,361.U,233.U,489.U,25.U,281.U,153.U,409.U,89.U,345.U,217.U,473.U,57.U,313.U,185.U,441.U,121.U,377.U,249.U,505.U,5.U,
  261.U,133.U,389.U,69.U,325.U,197.U,453.U,37.U,293.U,165.U,421.U,101.U,357.U,229.U,485.U,21.U,277.U,149.U,405.U,85.U,341.U,213.U,469.U,53.U,309.U,181.U,437.U,117.U,373.U,245.U,501.U,13.U,
  269.U,141.U,397.U,77.U,333.U,205.U,461.U,45.U,301.U,173.U,429.U,109.U,365.U,237.U,493.U,29.U,285.U,157.U,413.U,93.U,349.U,221.U,477.U,61.U,317.U,189.U,445.U,125.U,381.U,253.U,509.U,3.U,
  259.U,131.U,387.U,67.U,323.U,195.U,451.U,35.U,291.U,163.U,419.U,99.U,355.U,227.U,483.U,19.U,275.U,147.U,403.U,83.U,339
.U,211.U,467.U,51.U,307.U,179.U,435.U,115.U,371.U,243.U,499.U,11.U,
  267.U,139.U,395.U,75.U,331.U,203.U,459.U,43.U,299.U,171.U,427.U,107.U,363.U,235.U,491.U,27.U,283.U,155.U,411.U,91.U,347.U,219.U,475.U,59.U,315.U,187.U,443.U,123.U,379.U,251.U,507.U,7.U,
  263.U,135.U,391.U,71.U,327.U,199.U,455.U,39.U,295.U,167.U,423.U,103.U,359.U,231.U,487.U,23.U,279.U,151.U,407.U,87.U,343.U,215.U,471.U,55.U,311.U,183.U,439.U,119.U,375.U,247.U,503.U,15.U,
  271.U,143.U,399.U,79.U,335.U,207.U,463.U,47.U,303.U,175.U,431.U,111.U,367.U,239.U,495.U,31.U,287.U,159.U,415.U,95.U,351.U,223.U,479.U,63.U,319.U,191.U,447.U,127.U,383.U,255.U,511.U
))

  val output_correct = RegInit(false.B)
   
  val QINV = 12287
  val Q = 12289

  val poly_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  val factors_in = RegInit(VecInit(Seq.fill(512)(0.U(16.W))))
  
  val do_algo = RegInit(false.B)
  val poly_index = RegInit(0.U(8.W))
  val poly_out = Reg(Vec(512, UInt(16.W)))

  val do_initstep = RegInit(false.B)
  val do_aloop = RegInit(false.B)
  val do_aloop_init = RegInit(false.B)
  val do_aloop_for_s = RegInit(false.B)

  when (io.start && !do_algo) {
    do_algo := true.B
    do_initstep := true.B
    poly_in := io.poly_in
    withClockAndReset(clock, reset) {
      printf("ReversePoly poly input: 0x%x\n", Cat(io.poly_in))
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

    for (i <- 0 until 512) {
      val r = bitrev_table(i)
      when (i.U < r) {
        val tmp = poly_in(i)
        poly_in(i) := poly_in(r)
        poly_in(r) := tmp
      }
    }
    output_correct := true.B
  }

  when (!output_correct) {
    io.poly_out := VecInit(Seq.fill(512)(0.U(16.W)))
    io.output_valid := false.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("Reverse Poly Done: 0x%x\n", Cat(poly_in))
    }
    output_correct := false.B
    do_algo := false.B
    io.poly_out := poly_in
    // io.state_out := Vec(Seq.fill(512)(0.U(16.W)))
    io.output_valid := true.B
  }

  // while (input_offset < io.length_in) {

  // }
  // when ((io.length_in - input_offset) < rate) {

  // }
}

object ReversePoly {
  def apply(): ReversePoly = Module(new ReversePoly())
}
