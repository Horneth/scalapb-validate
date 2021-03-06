package scalapb.validate.compiler

import com.google.protobuf.ByteString
import com.google.protobuf.Descriptors.FieldDescriptor
import io.envoyproxy.pgv.validate.validate.BytesRules
import scalapb.validate.compiler.Rule.ifSet
import scalapb.validate.compiler.StringRulesGen.quoted

object BytesRuleGen {
  private val BV: String = "io.envoyproxy.pgv.BytesValidation"
  private val CV: String = "io.envoyproxy.pgv.ConstantValidation"

  def preamble(pname: String, v: ByteString) =
    s"private val $pname: Array[Byte] = Array[Byte](${v.toByteArray.map(_.toString).mkString(", ")})"

  def preambleByteString(pname: String, v: ByteString) =
    s"private val $pname: com.google.protobuf.ByteString = com.google.protobuf.ByteString.copyFrom(Array[Byte](${v.toByteArray.map(_.toString).mkString(", ")}))"

  def bytesRules(
      fd: FieldDescriptor,
      rules: BytesRules
  ): Seq[Rule] =
    Seq(
      rules.len.map(value => Rule.java(s"$BV.length", value.toString)),
      rules.minLen.map(value => Rule.java(s"$BV.minLength", value.toString)),
      rules.maxLen.map(value => Rule.java(s"$BV.maxLength", value.toString)),
      rules.const.map {
        val pname = s"Const_${fd.getName}"
        v =>
          Rule
            .java(s"$CV.constant", pname)
            .withPreamble(
              preambleByteString(pname, v)
            )
      },
      rules.prefix.map { (v: ByteString) =>
        val pname = s"Prefix_${fd.getName}"
        Rule
          .java(
            s"$BV.prefix",
            pname
          )
          .withPreamble(
            preamble(pname, v)
          )
      },
      rules.suffix.map { v =>
        val pname = s"Suffix_${fd.getName}"
        Rule
          .java(
            s"$BV.suffix",
            pname
          )
          .withPreamble(
            preamble(pname, v)
          )
      },
      rules.pattern.map { v =>
        val pname = s"Pattern_${fd.getName}"
        Rule
          .java(
            s"$BV.pattern",
            pname
          )
          .withPreamble(
            s"private val $pname = com.google.re2j.Pattern.compile(${quoted(v)})"
          )
      },
      ifSet(rules.getIp)(Rule.java(s"$BV.ip")),
      ifSet(rules.getIpv4)(Rule.java(s"$BV.ipv4")),
      ifSet(rules.getIpv6)(Rule.java(s"$BV.ipv6")),
      rules.contains.map { v =>
        val pname = s"Contains_${fd.getName}"
        Rule
          .java(
            s"$BV.contains",
            pname
          )
          .withPreamble(
            preamble(pname, v)
          )
      }
    ).flatten ++ MembershipRulesGen.membershipRules(rules)
}
