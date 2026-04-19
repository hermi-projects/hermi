Marker和Validator实现类必须是线程安全的（Thread-Safe）且无状态的（Stateless）。
框架会缓存并多线程复用此实例。

Same Reason to Change原则拆分文件
