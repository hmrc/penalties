/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package config.featureSwitches

sealed trait FeatureSwitch {
  val name: String
}

object FeatureSwitch {
  val prefix: String = "feature.switch"
  val listOfAllFeatureSwitches: List[FeatureSwitch] =
    List(CallPEGA, CallDES, CallAPI1812ETMP, CallAPI1811ETMP, CallAPI1811HIP, CallAPI1811Stub, SanitiseFileName, CallAPI1812HIP, CallAPI1808HIP)
}

case object CallPEGA extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-pega"
}

case object CallDES extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-des"
}

case object CallAPI1812ETMP extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1812-etmp"
}

case object CallAPI1811ETMP extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1811-etmp"
}

// HIP API#5327 replaces IF API#1811
case object CallAPI1811HIP extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1811-hip"
}
case object CallAPI1811Stub extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1811-stub"
}

case object SanitiseFileName extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.sanitise-file-name"
}

case object CallAPI1808HIP extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1808-hip"
}

case object CallAPI1812HIP extends FeatureSwitch {
  override val name: String = s"${FeatureSwitch.prefix}.call-api-1812-hip"
}