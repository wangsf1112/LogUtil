# LogUtil
android日志保存工具

使用前需要先初始化：
  LogUtil.init(context);
  
支持对日志信息是否logcat输出以及是否保存等配置
  LogUtil.setDebuggable(true);
  LogUtil.setSaveToLocal(true);

该工具使用类似Android系统的Log类，可输出不同日志级别：v、d、i、w、e。
  LogUtil.d("TAG", "msg");
