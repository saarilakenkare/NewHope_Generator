package newhope

import chisel3._
import chisel3.util._
import chisel3.util.random
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util._

class NewHopeAccel(opcodes: OpcodeSet)(implicit p: Parameters) extends LazyRoCC(
    opcodes = opcodes, nPTWPorts = 0) {
    override lazy val module = new NewHopeAccelImp(this)
}

class NewHopeAccelImp(outer: NewHopeAccel)(implicit p: Parameters) extends LazyRoCCModuleImp(outer) {
  val cipher_other = "ha65bd6669d3c113c07a7c0ad70a854d24aa280e6510046c5eb0808564f8471925ac5679f406545543926d0483e82e332285b6fe46a453156e8ed6dd542b63fd948881bd90d92878f6ee1d8bc63d102df362bd85bc090fcea4da217dfe52debe06c58b85b5ba4c6217f629833d8b62e6d16a93b434bbd7589d31962ac722f8f5e2143d272227ae91cf7435231e198509ab0d10a2d6b8b37f90228eacb627bbf527015dc3434b765700c4eb9d754880c911921467f362994483a47c624b7415fba83bafa3e4a853092b6894425e3afdacced7b78943cf25ade850c973d93d7f515acdcccfa741c924fca211902b4541950b023cf49127d9ad09558a85e8e8fea90c63475609874f11d4492d64beec587c050b790dd6b5e1a3856a1a32572da61275ba901062a5e379065e872301a34eb5ac899e840bd20dddaf2bdbfed21cc23a4091d6e61f999b5d518e6e333f2af18b0f7a0b2d7048600c01d175476163159eaae9293f54ccc5d29a21f323b08460f539361788ee8bed78fb48d63e946b51d3e6f2be30af094c039d0802795ca9d456601599b567e6432af70a521d42b85d1b292b42c127558e165b9f183368a7c77d8856167d29d876fb144d858de03389c4cd29c7d238151bc1a84435a9d3c10d9047587a49a815b1f6c56dc689de76e91fbb88a2eac501a098d397c4084ef937c55995f982a5a16e20d45648426af6651fad5070b2ebe252fd9b87a4b1647054485172a066fa6d62c5a0d086da4d45531aa50340fa31136a70593852be10237664ee90ca5c63c3558b8d01d89bd95344d09ebc960bc2116e762b56de47e226526d5dcb563ca5fa273e121896f2e99949d76a6d3277241474490da9ba6a6657d0f50906ad4abf19ea50d8af50f654cf20c1f3a1c926c15487100fb58af89450099bfc0a3304d8363f50c0d65caaa4851229127aff8a2ef48873295f538e03a6d545248e83ef0b56a99336883f4896180b75851c4585c4022b4ddfda6246aa9a2289409bde65e521acd69ee5d6257fd88a6a9069036012d9e631727499f5d4e3715b55a6a6a5d901fa583f6ea2db007858b492472d5a4b6f79741997e121b860a5846989c62f2955529a56ccdf3ece92efc00682b99c0741da9f7799ad81d2917bd57b2c408dc4c065a57b6b4d0daabc585c6990b274c0a45d057d091b1487e7c84cbc893d4556021c587dce62d48a287efc951a66163326f61f81704967ccaf9d18310926b542027097d4ae66e3e1694047f5462ccda5988ba669be65834e22c582b9bc9e8e57b8631bfb29712a15158b358655effab0d99ef3a6f22f06102323ad796c89a630c0bd34a2993eb991d61e34d07433d676d03ff9fb33dc5830ddc1b20bfffa4773fad2a3d5cdd1e348b9902aab25c7dfedbfbef8253c332902c2a0a9f2da50474eafa8f9911f0940480ccb56cdf004ad101f9fea2821d09b279f44c1204e32ae35e61801743d6453d914e45fb7a7fbf292ee6e9f9ee3975a56702f947603806dd9b9e0928ec642fe1a8f5fbf5".U

  //val ss = "h00d6b3820ed1e0f0123bc0973517f42c9bd5e3c165ecaf5b9e9a738b0c6879d7".U
  // val secret_key = "h2a02ba1beda04f106d2b66a662b9dc903dd64f2a203351b2c4334a8ad11b6654dfed7e8b8891d7002d8443c8fc48a75c9d039037e6bc92aac9c5a7802c45984fa593d1ec58aa4aae747bdc85a9dc1f24c0477d78d647b0e572096ece594b03c6ea94ada21a865aea40af10fa56e528870baa46293c50046126837af30abc9f596429816d7f162920a4860a75808878e20ca481782fc9f95a3c52c805196b112e69ac0bb81a081db319a93961868c95090fd45221b43fccd9d9691c0c516899414244257578e1586301f50dc09dd676dfac3ee05e7652d01c6e7dae2f373d516ebf5454169ff152568dc3d597c1a5cb20bc93e9e012472016b5de7428a2416f47fc3584864f1d96640c5c63c326f26ae50653ae487001298b7a0ee6d51116006e6ecb239b6e33acedf323d24ead4a8ccbc7e6523d2ec2a1f3a59a073d50dd14291a7367dfffa358b91a0fdaeb9068266edc9dba703e922fd65da052ba4164c2ed192399a00728927317d9b8bd665643781cfd0ba8a036798cb9338def95f345420a720fac73c39a6addefe626b8dc1c138ac457c6711234da3386e12000d7d024f8eac657ffec4b263021991f574366d08646342ce627d40447d8d7df23adc97a496658b44fd93b328dbcd385bdbe780de5aa101e5d56883331b8a92d212e68c2523aa084c632011878b72345cd209fc02a2fadbbb6248c914292028348468bea1ea22a56286e388ca94224c27d0ddac1b2d36195a8066665226e3004efd8da765c357f673ad26b0e4ab453c242fb894aa3c62d488888457501a342d82a85fe6e35cb36b2b013d8ad8aef6e21d12244c14b14073faa18a3294da962a86ba9b5f000a4eb8fa15f3528612193f3e775b399c735c067fd0bff95ba1742ac691a4b18d030f04b4a2a85c57d20bb594faaf32bee013d6bb0c18b0846695cf5277422341cdd95f0a7b42133067fc56626768762c943d99c7798595712cbe6621ad65ce86434916925140a674021021528b40f72906a018af480d0740f63494b123134051935c82ef54b93ea3726a84e3a3ee24315c4197e3dcd7dd9f821356c4681c89c745ed226b05900d1a71121a41899492b424066d0a1aa82d5573b3a26d671cccdf368deeeb399dadfeb48528d56590a3533e54925a82e423a7e9273cdb267e38e5f48d850baf09d7e56d21794100c64b14bcdc1874179b1822f0475d6b5deabc4db8f465f58a01426717e4bbb5d9199f4658883c664f7994c8d0f1115845309b9".U

  val public_key_hardcoded = "ha65bd6669d3c113c07a7c0ad70a854d24aa280e6510046c5eb0808564f8471925ac5679f406545543926d0483e82e332285b6fe46a453156e8ed6dd542b63fd948881bd90d92878f6ee1d8bc63d102df362bd85bc090fcea4da217dfe52debe06c58b85b5ba4c6217f629833d8b62e6d16a93b434bbd7589d31962ac722f8f5e2143d272227ae91cf7435231e198509ab0d10a2d6b8b37f90228eacb627bbf527015dc3434b765700c4eb9d754880c911921467f362994483a47c624b7415fba83bafa3e4a853092b6894425e3afdacced7b78943cf25ade850c973d93d7f515acdcccfa741c924fca211902b4541950b023cf49127d9ad09558a85e8e8fea90c63475609874f11d4492d64beec587c050b790dd6b5e1a3856a1a32572da61275ba901062a5e379065e872301a34eb5ac899e840bd20dddaf2bdbfed21cc23a4091d6e61f999b5d518e6e333f2af18b0f7a0b2d7048600c01d175476163159eaae9293f54ccc5d29a21f323b08460f539361788ee8bed78fb48d63e946b51d3e6f2be30af094c039d0802795ca9d456601599b567e6432af70a521d42b85d1b292b42c127558e165b9f183368a7c77d8856167d29d876fb144d858de03389c4cd29c7d238151bc1a84435a9d3c10d9047587a49a815b1f6c56dc689de76e91fbb88a2eac501a098d397c4084ef937c55995f982a5a16e20d45648426af6651fad5070b2ebe252fd9b87a4b1647054485172a066fa6d62c5a0d086da4d45531aa50340fa31136a70593852be10237664ee90ca5c63c3558b8d01d89bd95344d09ebc960bc2116e762b56de47e226526d5dcb563ca5fa273e121896f2e99949d76a6d3277241474490da9ba6a6657d0f50906ad4abf19ea50d8af50f654cf20c1f3a1c926c15487100fb58af89450099bfc0a3304d8363f50c0d65caaa4851229127aff8a2ef48873295f538e03a6d545248e83ef0b56a99336883f4896180b75851c4585c4022b4ddfda6246aa9a2289409bde65e521acd69ee5d6257fd88a6a9069036012d9e631727499f5d4e3715b55a6a6a5d901fa583f6ea2db007858b492472d5a4b6f79741997e121b860a5846989c62f2955529a56ccdf3ece92efc00682b99c0741da9f7799ad81d2917bd57b2c408dc4c065a57b6b4d0daabc585c6990b274c0a45d057d091b1487e7c84cbc893d4556021c587dce63d48a287efc951a66163326f61f81704967ccaf9d18310926b542027097d4ae66e3e1694047ff8ac70df871851eb09b1096e0f2ef9a07ebb26895ab866ed238db0f42b1438a5".U

  val cmd = Queue(io.cmd)
  val funct = cmd.bits.inst.funct
  val rd = RegInit(0.U(32.W))
  val rs1 = RegInit(0.U(32.W))
  
  val secret_key = RegInit(0.U((8*896).W))
  val public_key = RegInit(0.U((8*928).W))
  val cipher = RegInit(0.U((8*1088).W))
  val ss = RegInit(0.U((8*32).W))
  // val shake_input = Wire(UInt(width = 32.W))
  // shake_input := 0.U

  val doResp = RegInit(false.B)
  val key_gen_started = RegInit(false.B)
  // val key_gen_started = RegInit(false.B)
  val key_gen_finished = RegInit(false.B)
  val key_encaps_started = RegInit(false.B)
  val key_encaps_finished = RegInit(false.B)
  val key_decaps_started = RegInit(false.B)
  val key_decaps_finished = RegInit(false.B)
  val done = RegInit(false.B)
  val sent = RegInit(false.B)
  when (cmd.valid && funct === 0.U) {
    withClockAndReset(clock, reset) {
      printf("Function: %d\n", funct)
    }
    // cmd.ready := true.B
    // io.busy := true.B
    // key_encaps_started := true.B
    // key_gen_started := true.B
    key_gen_started := true.B
    // rd := cmd.bits.rs1
    rd := cmd.bits.inst.rd
    rs1 := cmd.bits.rs1
    // key_decaps_started := true.B
  }
  //.otherwise {
    // cmd.ready := false.B
    // io.busy := false.B
  //  key_gen_started := false.B
  //}
  
  /*when (key_gen_started && !key_gen_finished) {
    val KeyGenModule = Test()
    KeyGenModule.io.start := key_gen_started
    when (KeyGenModule.io.done) {
      withClockAndReset(clock, reset) {
        printf("Key Gen Module Done!\n")
      }
      withClockAndReset(clock, reset) {
        printf("case 1\n")
        // printf("case 1: cmd.ready = false, io.busy = true\n")
      }
      key_gen_started := false.B
      key_gen_finished := true.B
      done := true.B
      cmd.ready := false.B
      io.busy := true.B

    }
    .otherwise {
      cmd.ready := false.B
      io.busy := true.B
    }
  }*/

  //val rand = chisel3.util.random.LFSR(32, false.B, Some(1))
  //withClockAndReset(clock, reset) {
  //  printf("rand: 0x%x\n", rand)
  //}

  val KeyGenModule = KeyGen()
  KeyGenModule.io.start := key_gen_started
  
  //val KeyEncapsModule = KeyEncaps()
  //KeyEncapsModule.io.start := key_gen_started
  //KeyEncapsModule.io.public_key := public_key_hardcoded
  
  //when (KeyEncapsModule.io.done) {
  when (KeyGenModule.io.done) {
    withClockAndReset(clock, reset) {
      printf("Key Encaps Module Done!\n")
    }
    key_gen_finished := true.B
    doResp := true.B
  }

  when (!key_gen_started && !key_gen_finished) {
    withClockAndReset(clock, reset) {
      printf("Not received message yet.\n")
    }
    //cmd.ready := true.B
    //io.busy := false.B
  }
  .elsewhen (key_gen_started && !key_gen_finished) {
    withClockAndReset(clock, reset) {
      printf("Working on key gen.\n")
    }
    //cmd.ready := false.B
    //io.busy := true.B
  }
  .otherwise {
    withClockAndReset(clock, reset) {
      printf("???\n")
    }
    //cmd.ready := true.B
    //io.busy := false.B

  }

  io.resp.valid := cmd.valid 
  io.resp.bits.rd := cmd.bits.inst.rd
  io.resp.bits.data := 0.U

  cmd.ready := (!key_gen_started) || (key_gen_finished)
  io.busy := key_gen_started && !key_gen_finished && !sent


  when (io.mem.req.fire()) {
    withClockAndReset(clock, reset) {
      printf("!!!\n")
    }
    sent := true.B
  }
  
  when (io.mem.resp.valid) {
    //busy(io.mem.resp.bits.tag) := false.B
    withClockAndReset(clock, reset) {
      printf("valid memory response!\n")
      printf("tag: %d\n", io.mem.resp.bits.tag)
      printf("response: 0x%x\n", io.mem.resp.bits.data)
    }
  }

  io.mem.req.valid := key_gen_finished && !sent
  io.mem.req.bits.addr := rs1
  io.mem.req.bits.tag := 1.U
  io.mem.req.bits.cmd := M_XWR // perform a load (M_XWR for stores)
  io.mem.req.bits.size := 64.U
  io.mem.req.bits.signed := false.B
  io.mem.req.bits.data := "h12345678".U // we're not performing any stores...
  io.mem.req.bits.phys := false.B

  //cmd.ready := !key_gen_started
  //io.busy := key_gen_started && !key_gen_finished

  /*when (key_gen_started && !key_gen_finished) {
    val KeyGenModule = KeyGen()
    KeyGenModule.io.start := key_gen_started
    when (KeyGenModule.io.done) {
      withClockAndReset(clock, reset) {
        printf("Key Gen Module Done!\n")
        printf("Public Key: 0x%x\n", KeyGenModule.io.public_key)
      }
      withClockAndReset(clock, reset) {
        printf("case 1\n")
        // printf("case 1: cmd.ready = false, io.busy = true\n")
      }
      public_key := KeyGenModule.io.public_key
      secret_key := KeyGenModule.io.secret_key
      done := true.B
      key_gen_started := false.B
      key_gen_finished := true.B
      // key_encaps_started := true.B
      //cmd.ready := false.B
      //io.busy := true.B

      KeyGenModule.io.start := false.B
    }
    .otherwise {
      withClockAndReset(clock, reset) {
        printf("case 2\n")
      }
      //cmd.ready := false.B
      //io.busy := true.B
    }
  }*/
  /*.elsewhen (key_encaps_started && !key_encaps_finished) {
    val KeyEncapsModule = KeyEncaps()
    KeyEncapsModule.io.start := key_encaps_started
    KeyEncapsModule.io.public_key := public_key
    when (KeyEncapsModule.io.done) {
      withClockAndReset(clock, reset) {
        printf("case 4\n")
        // printf("case 1: cmd.ready = false, io.busy = true\n")
      }
      withClockAndReset(clock, reset) {
        printf("Key Encaps Module Done!\n")
        // val last = KeyEncapsModule.io.cipher & "hFFFF".U
        // printf("cipher(0): 0x%x\n", last) 
      }
      key_encaps_started := false.B
      key_encaps_finished := true.B
      // done := true.B
      key_decaps_started := true.B
      cmd.ready := false.B
      io.busy := true.B
      cipher := KeyEncapsModule.io.cipher
      // ss := KeyEncapsModule.io.ss
    }
    .otherwise {
      cmd.ready := false.B
      io.busy := true.B
    }
  }
  .elsewhen (key_decaps_started && !key_decaps_finished) {
    val KeyDecapsModule = KeyDecaps()
    KeyDecapsModule.io.start := key_decaps_started
    KeyDecapsModule.io.ciphertext := cipher
    KeyDecapsModule.io.secret := secret_key
    when (KeyDecapsModule.io.done) {
      withClockAndReset(clock, reset) {
        printf("Key Decaps Module Done!\n")
      }
      withClockAndReset(clock, reset) {
        printf("case 1\n")
        // printf("case 1: cmd.ready = false, io.busy = true\n")
      }
      done := true.B
      key_decaps_started := false.B
      key_decaps_finished := true.B
      cmd.ready := false.B
      io.busy := true.B

      KeyDecapsModule.io.start := false.B
    }
    .otherwise {
      withClockAndReset(clock, reset) {
        printf("case 2\n")
      }
      cmd.ready := false.B
      io.busy := true.B
    }
  }
  */
  //.elsewhen (!key_encaps_finished && key_encaps_started) {
    //withClockAndReset(clock, reset) {
      //printf("case 3\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    //}
    //cmd.ready := false.B
    //io.busy := true.B
  //}
  /*.otherwise {
    withClockAndReset(clock, reset) {
      printf("case 5\n")
      // printf("case 1: cmd.ready = false, io.busy = true\n")
    }
    withClockAndReset(clock, reset) {
      // printf("case 3: cmd.ready = false, io.busy = false\n")
    }
    cmd.ready := false.B
    io.busy := false.B
  }*/

  //cmd.ready := !key_gen_started
  //io.busy := key_gen_started

  withClockAndReset(clock, reset) {
    printf("cmd.valid: %d\n", cmd.valid)
    printf("funct: %d\n", funct)
    printf("key_gen_started: %d\n", key_gen_started)
    printf("key_gen_finished: %d\n", key_gen_finished)
    printf("cmd.ready: %d\n", cmd.ready)
    printf("io.busy: %d\n", io.busy)
    printf("rd: 0x%x\n", rd)
    printf("rs1: 0x%x\n", rs1)
    // printf("Shake256Module out: 0x%x\n", Shake256Module.io.state_out)
  }

  // io.busy := cmd.valid
  io.interrupt := false.B
  //cmd.ready := !key_gen_started
  //io.busy := key_gen_started

  //io.resp.valid := doResp //&& !stallReg
    // valid response if valid command, need a response, and no stalls
  //io.resp.bits.rd := rd
    // Must respond with the appropriate tag or undefined behavior
  //io.resp.bits.data := "h12345678".U
  
  // MEMORY REQUEST INTERFACE
  // io.mem.req.valid := cmd.valid
  // io.mem.req.bits.addr := addr0 + (counterValue)
  // io.mem.req.bits.tag := (counter / 8.U)
  // io.mem.req.bits.cmd := memValue // perform a load (M_XWR for stores)
  // io.mem.req.bits.size := log2Ceil(8).U
  // io.mem.req.bits.signed := Bool(false)
  // io.mem.req.bits.data := 0.U // we're not performing any stores...
  // io.mem.req.bits.phys := Bool(false)

}

class StoreTestModule(outer: NewHopeAccel) extends LazyRoCCModuleImp(outer) {

  val regfile = Mem(4, UInt(width = 16.W))
  //val busy = RegInit(VecInit(Seq.fill(4)(false.B)))

  val cmd = Queue(io.cmd)
  val funct = cmd.bits.inst.funct
  val addr = 0.U
  val doStore = RegInit(false.B)
  val memRespTag = io.mem.resp.bits.tag(log2Up(4)-1,0)

  val storeAddr = RegInit(0.U(32.W))

  when (cmd.valid && funct === 0.U) {
    doStore := true.B
    storeAddr := cmd.bits.rs1
  }

  when (io.mem.resp.valid) {
    regfile(memRespTag) := io.mem.resp.bits.data
    //busy(memRespTag) := false.B
  }

  // control
  when (io.mem.req.fire()) {
    //busy(addr) := true.B
  }

  when (io.mem.resp.valid) {
    //busy(io.mem.resp.bits.tag) := false.B
    withClockAndReset(clock, reset) {
      printf("valid memory response!\n")
      printf("tag: %d\n", io.mem.resp.bits.tag)
      printf("response: 0x%x\n", io.mem.resp.bits.data)
    }
    doStore := false.B
  }

  val doResp = cmd.bits.inst.xd
  //val stallReg = busy(addr)

  cmd.ready := !doStore
  // command resolved if no stalls AND not issuing a load that will need a request

  // PROC RESPONSE INTERFACE
  io.resp.valid := cmd.valid && doResp //&& !stallReg
    // valid response if valid command, need a response, and no stalls
  io.resp.bits.rd := cmd.bits.inst.rd
    // Must respond with the appropriate tag or undefined behavior
  io.resp.bits.data := "h12345678".U
    // Semantics is to always send out prior accumulator register value

  io.busy := cmd.valid //|| busy.reduce(_||_)
    // Be busy when have pending memory requests or committed possibility of pending requests
  io.interrupt := false.B
    // Set this true to trigger an interrupt on the processor (please refer to supervisor documentation)

  // MEMORY REQUEST INTERFACE
  /*io.mem.req.valid := doStore
  io.mem.req.bits.addr := storeAddr
  io.mem.req.bits.tag := addr
  io.mem.req.bits.cmd := M_XWR // perform a load (M_XWR for stores)
  io.mem.req.bits.size := log2Ceil(8).U
  io.mem.req.bits.signed := false.B
  io.mem.req.bits.data := "h87654321".U // we're not performing any stores...
  io.mem.req.bits.phys := false.B
  */
  withClockAndReset(clock, reset) {
    printf("cmd.ready: %d\n", cmd.ready)
    printf("cmd.valid: %d\n", cmd.valid)
    printf("io.busy: %d\n", io.busy)
    printf("doStore: %d\n", doStore)
    printf("storeAddr: 0x%x\n", storeAddr)
    printf("addr: 0x%x\n", addr)
  }
}  


class WithNewHopeAccel extends Config ((site, here, up) => {
  case BuildRoCC => Seq(
    (p: Parameters) => {
      val newhope = LazyModule.apply(new NewHopeAccel(OpcodeSet.custom2)(p))
      newhope
    }
  )
})

