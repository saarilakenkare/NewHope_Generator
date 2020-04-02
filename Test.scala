package newhope

import chisel3._
import chisel3.util._

// implements ShiftRows
class Test extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
  })

  val public_key = RegInit("ha65bd6669d3c113c07a7c0ad70a854d24aa280e6510046c5eb0808564f8471925ac5679f406545543926d0483e82e332285b6fe46a453156e8ed6dd542b63fd948881bd90d92878f6ee1d8bc63d102df362bd85bc090fcea4da217dfe52debe06c58b85b5ba4c6217f629833d8b62e6d16a93b434bbd7589d31962ac722f8f5e2143d272227ae91cf7435231e198509ab0d10a2d6b8b37f90228eacb627bbf527015dc3434b765700c4eb9d754880c911921467f362994483a47c624b7415fba83bafa3e4a853092b6894425e3afdacced7b78943cf25ade850c973d93d7a5abacdcccfa741c924fca211902b4541950b023cf49127d9ad09558a85e8e8fea90c63475609874f11d4492d64beec587c050b790dd6b5e1a3856a1a32572da61275ba901062a5e379065e872301a34eb5ac899e840bd20dddaf2bdbfed21cc23a4091d6e61f999b5d518e6e333f2af18b0f7a0b2d7048600c01d175476163159eaae9293f54ccc5d29a21f323b08460f539361788ee8bed78fb48d63e946b51d3e6f2be30af094c039d0802795ca9d456601599b567e6432af70a521d42b85d1b292b42c127558e165b9f183368a7c77d8856167d29d876fb144d858de03389c4cd29c7d238151bc1a84435a9d3c10d9047587a49a815b1f6c56dc689de76e91fbb88a2eac501a098d397c4084ef937c55995f982a5a16e20d45648426af6651fad5070b2ebe252fd9b87a4b1647054485172a066fa6d62c5a0d086da4d45531aa50340fa31136a70593852be10237664ee90ca5c63c3558b8d01d89bd95344d09ebc960bc2116e762b56de47e226526d5dcb563ca5fa273e121896f2e99949d76a6d3277241474490da9ba6a6657d0f50906ad4abf19ea50d8af50f654cf20c1f3a1c926c15487100fb58af89450099bfc0a3304d8363f50c0d65caaa4851229127aff8a2ef48873295f538e03a6d545248e83ef0b56a99336883f4896180b75851c4585c4022b4ddfda6246aa9a2289409bde65e521acd69ee5d6257fd88a6a9069036012d9e631727499f5d4e3715b55a6a6a5d901fa583f6ea2db007858b492472d5a4b6f79741997e121b860a5846989c62f2955529a56ccdf3ece92efc00682b99c0741da9f7799ad81d2917bd57b2c408dc4c065a57b6b4d0daabc585c6990b274c0a45d057d091b1487e7c84cbc893d4556021c587dce62d48a287efc951a66163326f61f81704967ccaf9d18310926b542027097d4ae66e3e1694047ff8ac70df871851eb09b1096e0f2ef9a07ebb26895ab866ed238db0f42b1438a5".U((8*928).W))

  val seed =  RegInit("hf8ac70df871851eb09b1096e0f2ef9a07ebb26895ab866ed238db0f42b1438a5cc8e4fb060095f8366fa6de93ee8a0289d78c23db8afc121725898eb773e5dba".U(512.W))
  val decode_pk_true = RegInit(false.B)
  val done = RegInit(false.B)

  
  //val DecodePolyModule = DecodePoly()
  //DecodePolyModule.io.start := decode_pk_true
  //DecodePolyModule.io.public_key := public_key

  val MessageToPolynomialModule = MessageToPolynomial()
  MessageToPolynomialModule.io.start := decode_pk_true
  MessageToPolynomialModule.io.state_in := seed

  val do_algo = RegInit(false.B)

  when (io.start && !do_algo) {
    withClockAndReset(clock, reset) {
        printf("Starting Key Encaps\n")
        printf("Public Key: 0x%x\n", public_key)
    }
    decode_pk_true := true.B
    do_algo := true.B
  }

  when (decode_pk_true) {
    decode_pk_true := false.B
  }

  when (MessageToPolynomialModule.io.output_valid) {
    withClockAndReset(clock, reset) {
      printf("Received m2p message: 0x%x\n", Cat(MessageToPolynomialModule.io.poly_out))
    }
    done := true.B
    // gen_a_true := true.B
  }

  io.done := done
}

object Test {
  def apply(): Test = Module(new Test())
}
