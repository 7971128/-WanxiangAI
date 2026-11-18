import com.example.wanxiangai.SanfenStrategy

fun main() {
  check(SanfenStrategy.recommend(1, "弈星 曹操", "稳血").candidates.contains("弈星"))
  check(SanfenStrategy.recommend(4, "曹操 甄姬", "成型").candidates.contains("甄姬"))
  check(SanfenStrategy.recommend(7, "露娜 小乔", "运营").phase.contains("成型"))
  println("PASS: 3 strategy smoke tests")
}
