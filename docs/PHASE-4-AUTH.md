# Phase 4 — auth 认证 实施计划

> 日期：2026-06-17 | 状态：计划已审批，待编码

---

## 一、定位

Auth 不是业务子域，是**技术横切关注点**。负责：

| 能力 | 说明 |
|---|---|
| 用户认证 | 注册、登录，Sa-Token JWT 签发 token |
| 管理员认证 | 管理员独立登录端点 |
| 请求上下文注入 | UserContext (TTL) + 拦截器，后续所有子域通过 `UserContext.get()` 获取当前用户 |

---

## 二、要实现哪些功能，怎么实现

### 功能 1：用户注册

**做什么**：新用户用手机号+密码+昵称注册，成功后自动登录并返回 token。

**完整调用链路**：

```
POST /auth/register  {phone, password, nickname}

AuthController.register(dto)
  │
  ├─① 参数校验
  │   手机号为空 || 密码为空 → throw AuthException("手机号和密码不能为空")
  │
  ├─② 查重
  │   UserPort.findByPhone(dto.phone)
  │   └→ SQL: SELECT * FROM users WHERE phone=?
  │   已存在 → throw AuthException("手机号已注册")
  │
  ├─③ 密码哈希
  │   PasswordUtils.hash(dto.password)
  │   └→ SHA-256(plaintext) → 64位 hex 字符串
  │
  ├─④ 构建实体
  │   UserEntity.builder()
  │     .phone(dto.phone)
  │     .passwordHash(hash)
  │     .nickname(dto.nickname != null ? dto.nickname : "用户" + timestamp%10000)
  │     .role(UserRole.USER)
  │     .status("ACTIVE")
  │     .createdAt(NOW())
  │
  ├─⑤ 持久化
  │   UserPort.save(entity)
  │   └→ SQL: INSERT INTO users (phone, password_hash, nickname, role, status, created_at)
  │        VALUES (?, ?, ?, 'USER', 'ACTIVE', NOW())
  │
  ├─⑥ 签发 token
  │   StpUtil.login(entity.getId())
  │   token = StpUtil.getTokenValue()
  │   │
  │   内部: Sa-Token 生成 JWT，payload 含 userId，有效期 7 天
  │
  └─⑦ 返回
      Response.ok({token, role:"USER", nickname})
```

---

### 功能 2：用户登录

**做什么**：已注册用户用手机号（或用户名）+密码登录，返回 token。

**完整调用链路**：

```
POST /auth/login  {account, password}

AuthController.login(dto)
  │
  ├─① 参数校验
  │   account 为空 || password 为空 → throw AuthException("账号和密码不能为空")
  │
  ├─② 识别账号类型 → 查用户
  │
  │   account 匹配手机号正则 (^1\d{10}$):
  │     UserPort.findByPhone(account)
  │     └→ SQL: SELECT * FROM users WHERE phone=?
  │
  │   否则 (用户名):
  │     UserPort.findByNickname(account)
  │     └→ SQL: SELECT * FROM users WHERE nickname=?
  │
  │   null → throw AuthException("账号或密码错误")
  │
  ├─③ 验密
  │   PasswordUtils.hash(dto.password) == user.getPasswordHash()
  │   不匹配 → throw AuthException("账号或密码错误")
  │   (统一提示"账号或密码错误"，不区分具体原因)
  │
  ├─④ 更新最后登录时间
  │   user.setLastLoginAt(NOW())
  │   UserPort.save(user)  或直接 UPDATE
  │
  ├─⑤ 签发 token
  │   StpUtil.login(user.getId())
  │   StpUtil.getSession().set("role", user.getRole().name())
  │
  └─⑥ 返回
      Response.ok({token, role, nickname})
```

---

### 功能 3：管理员登录

**做什么**：管理员用用户名+密码登录，返回 token。独立于用户系统。

**完整调用链路**：

```
POST /admin/auth/login  {username, password}

AdminAuthController.login(request)
  │
  ├─① 参数校验
  │   username 为空 || password 为空 → throw AuthException("用户名和密码不能为空")
  │
  ├─② 查管理员
  │   AdminUserPort.findByUsername(username)
  │   └→ SQL: SELECT * FROM admin_users WHERE username=?
  │   null → throw AuthException("用户名或密码错误")
  │
  ├─③ 验密
  │   PasswordUtils.hash(password) == admin.getPasswordHash()
  │
  ├─④ 签发 token
  │   StpUtil.login(admin.getId())
  │   StpUtil.getSession().set("role", "ADMIN")
  │   StpUtil.getSession().set("adminId", admin.getId())
  │
  └─⑤ 返回
      {token, username, role:"ADMIN"}
```

---

### 功能 4：请求拦截注入用户上下文

**做什么**：每个请求进入时，从 Header 取 token → 解析出 userId → 查库 → 注入 ThreadLocal。后续所有代码直接 `UserContext.getUserId()` 获取当前用户。

**完整调用链路**：

```
任意 HTTP 请求进入
  │
  ▼
UserContextInterceptor.preHandle(request, response, handler)
  │
  ├─① OPTIONS 预检 → return true（直接放行，不解析 token）
  │
  ├─② 检查路由白名单
  │   /auth/**, /admin/auth/**, /doc.html, /v3/**, /error
  │   → 白名单路由不经过此拦截器（WebMvcConfig 已排除）
  │
  ├─③ 从 Header 取 token
  │   String token = request.getHeader("Authorization")
  │   token 为空 → throw AuthException("未登录")
  │
  ├─④ Sa-Token 解析
  │   Object loginId = StpUtil.getLoginIdByToken(token)
  │   (Sa-Token 内部验签 JWT + 过期检查)
  │   null 或异常 → throw AuthException("token 无效或已过期")
  │   Long userId = (Long) loginId
  │
  ├─⑤ 查用户信息
  │   UserPort.findById(userId)
  │   └→ SQL: SELECT * FROM users WHERE id=?
  │   null → throw AuthException("用户不存在")
  │
  ├─⑥ 注入 ThreadLocal
  │   UserContext.set(userId, user.getRole())
  │   └→ ThreadLocal.set(new UserContext(userId, role))
  │       (TTL 版: 支持父子线程传递)
  │
  └─⑦ return true → 请求继续

  ═══ 业务处理 ═══

  afterCompletion:
    UserContext.clear()
    └→ ThreadLocal.remove() 防止内存泄漏
```

**业务代码中使用**：
```java
// 任意 Controller / Service 中:
Long userId = UserContext.getUserId();
String role = UserContext.getRole();

// 无需从参数传入 userId，无需手动 StpUtil.getLoginIdAsLong()
```

---

### 功能 5：Token 配置

**做什么**：Sa-Token 的全局配置——JWT 模式、7 天过期、路由白名单。

**SaTokenConfig**：
```java
@Configuration
public class SaTokenConfig {
    // ① JWT 模式: sa-token.token-style=jwt
    // ② token 有效期: sa-token.timeout=604800 (7天)
    // ③ token 从 Header 读取: sa-token.token-name=Authorization
    // ④ 路由排除（注解式）:
    //    @SaIgnore 标注在 IAuthService 接口上
    //    或配置: sa-token.exclude-path=/auth/**,/admin/auth/**,/doc.html,/v3/**,/error
}
```

---

## 三、涉及数据库表

| 表 | 操作 | 关键查询 |
|---|---|---|
| `users` | 插入、查询 | `findByPhone`、`findById` |
| `admin_users` | 查询 | `findByUsername` |

---

## 四、类清单与关系

```
┌──────────────────────────────────────────────────────────────────┐
│                   recite-api (REST 契约)                          │
│                                                                   │
│  IAuthService      ← /auth/register, /auth/login                 │
│  DTO × 3           ← LoginRequestDTO, RegisterRequestDTO,         │
│                        LoginResultDTO                             │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│                   recite-trigger (控制器 + 配置)                   │
│                                                                   │
│  http/                                                            │
│  ├─ AuthController        实现 IAuthService                       │
│  └─ AdminAuthController   管理员登录                              │
│                                                                   │
│  config/                                                          │
│  ├─ SaTokenConfig         Sa-Token JWT 配置                       │
│  ├─ UserContext           TTL ThreadLocal                         │
│  ├─ UserContextInterceptor  token→查用户→注入 TTL                  │
│  └─ WebMvcConfig          注册拦截器 + 路由白名单                  │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 依赖注入
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│            recite-domain/auth (领域层，不是子域)                   │
│                                                                   │
│  model/entity/                                                    │
│  ├─ UserEntity          用户实体                                  │
│  └─ AdminUserEntity     管理员实体                                │
│                                                                   │
│  port/out/                                                        │
│  ├─ UserPort            用户持久化 SPI                            │
│  └─ AdminUserPort       管理员持久化 SPI                          │
│                                                                   │
│  exception/                                                       │
│  └─ AuthException       认证异常                                  │
└──────────────────────────┬───────────────────────────────────────┘
                           │ 实现
┌──────────────────────────▼───────────────────────────────────────┐
│             recite-infrastructure/adapter/persistence/            │
│                                                                   │
│  UserPersistenceAdapter      implements UserPort                  │
│    └→ UserDO + UserMapper                                        │
│                                                                   │
│  AdminUserPersistenceAdapter implements AdminUserPort             │
│    └→ AdminUserDO + AdminUserMapper                              │
└──────────────────────────────────────────────────────────────────┘
```

---

## 五、每个类/接口的详细职责

### 5.1 领域实体

#### UserEntity
```java
// 字段
Long id;
String phone;           // 手机号，唯一
String passwordHash;    // SHA-256 哈希
String nickname;        // 昵称
String avatar;          // 头像 URL
String role;            // USER / ADMIN
String status;          // ACTIVE / DISABLED
LocalDateTime lastLoginAt;
LocalDateTime createdAt;
```

#### AdminUserEntity
```java
// 字段
Long id;
String username;        // 登录名
String passwordHash;    // SHA-256 哈希
String nickname;
String status;          // ACTIVE / DISABLED
LocalDateTime createdAt;
```

### 5.2 Port 接口（SPI）

#### UserPort
```java
public interface UserPort {
    UserEntity findByPhone(String phone);          // 手机号查用户（注册去重用）
    UserEntity findById(Long id);                  // 按 ID 查（拦截器注入用）
    void save(UserEntity user);                    // 新增用户
}
```

#### AdminUserPort
```java
public interface AdminUserPort {
    AdminUserEntity findByUsername(String username);  // 管理员登录查询
}
```

### 5.3 异常

#### AuthException
```java
// 继承 AppException
// 场景：手机号已注册、账号或密码错误、账号被禁用、未登录、token 失效
```

### 5.4 REST 接口

#### IAuthService
```
路径前缀: /auth
```

| # | 方法 | HTTP | 路径 | 请求体 | 返回 |
|:--:|------|------|------|------|------|
| 1 | `register` | POST | `/register` | `RegisterRequestDTO` | `Response<LoginResultDTO>` |
| 2 | `login` | POST | `/login` | `LoginRequestDTO` | `Response<LoginResultDTO>` |

#### AdminAuthController
```
路径: /admin/auth
```

| # | 方法 | HTTP | 路径 | 请求体 | 返回 |
|:--:|------|------|------|------|------|
| 1 | `login` | POST | `/login` | `Map{username, password}` | `Response<Map{token, username}>` |

### 5.5 DTO

| DTO | 字段 | 用途 |
|---|---|---|
| `LoginRequestDTO` | account (String), password (String) | 登录请求（支持手机号或用户名） |
| `RegisterRequestDTO` | phone, password, nickname | 注册请求 |
| `LoginResultDTO` | token, role, nickname | 登录/注册成功响应 |

### 5.6 配置类（trigger/config/）

#### SaTokenConfig
```java
@Configuration
public class SaTokenConfig {
    // Sa-Token JWT 模式
    // token 过期: 7 天
    // token 名称: Authorization (Header)
    // 路由排除: /auth/**, /admin/auth/**, /doc.html, /v3/**, /error
}
```

#### UserContext
```java
// TTL (TransmittableThreadLocal) 包装
public final class UserContext {
    private static final ThreadLocal<UserContext> HOLDER = new TransmittableThreadLocal<>();

    private Long userId;
    private String role;

    // 静态方法
    public static void set(Long userId, String role) { ... }
    public static UserContext get() { ... }       // 返回当前上下文，null 表示未登录
    public static Long getUserId() { ... }        // 便捷方法
    public static String getRole() { ... }        // 便捷方法
    public static void clear() { HOLDER.remove(); }
}
```

#### UserContextInterceptor
```java
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Autowired
    private UserPort userPort;

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        // 1. OPTIONS 预检请求直接放行
        // 2. 从 Authorization Header 取 token
        // 3. Sa-Token 解析 token → loginId (userId)
        // 4. userPort.findById(userId) → UserEntity
        // 5. UserContext.set(userId, role)
        // 6. return true
    }

    @Override
    public void afterCompletion(...) {
        UserContext.clear();
    }
}
```

#### WebMvcConfig
```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor)
                .excludePathPatterns("/auth/**", "/admin/auth/**",
                                     "/doc.html", "/v3/**", "/error");
    }
}
```

### 5.7 控制器

#### AuthController
- 实现 `IAuthService`
- 注入 `UserPort`
- `register()`: 校验手机号唯一 → `PasswordUtils.hash(password)` → `userPort.save()` → `StpUtil.login(userId)` → 返回 token
- `login()`: 支持手机号或用户名 → 查用户 → 验哈希 → 更新 `lastLoginAt` → 签发 token

#### AdminAuthController
- 注入 `AdminUserPort`
- `login()`: 验用户名密码 → `StpUtil.login(adminId)` → 返回 token+username

### 5.8 基础设施适配器

| 适配器 | 实现 | 技术 |
|---|---|---|
| `UserPersistenceAdapter` | `UserPort` | MyBatis Plus，操作 `users` 表 |
| `AdminUserPersistenceAdapter` | `AdminUserPort` | MyBatis Plus，操作 `admin_users` 表 |

### 5.9 持久层

| 类 | 映射表 | 说明 |
|---|---|---|
| `UserDO` | `users` | MyBatis Plus PO |
| `UserMapper` | `users` | `extends BaseMapper<UserDO>` |
| `AdminUserDO` | `admin_users` | MyBatis Plus PO |
| `AdminUserMapper` | `admin_users` | `extends BaseMapper<AdminUserDO>` |

### 5.10 已有可直接用的（recite-types）

| 类 | 用途 |
|---|---|
| `PasswordUtils.hash(password)` | SHA-256 哈希 |
| `UserRole` 枚举 | USER / ADMIN |
| `ResponseCode.UNAUTHORIZED` | 401 |
| `ResponseCode.FORBIDDEN` | 403 |

---

## 六、依赖注入关系

```
AuthController
  └─ @Autowired UserPort → UserPersistenceAdapter

AdminAuthController
  └─ @Autowired AdminUserPort → AdminUserPersistenceAdapter

UserContextInterceptor
  └─ @Autowired UserPort → UserPersistenceAdapter

WebMvcConfig
  └─ @Autowired UserContextInterceptor
```

---

## 七、文件清单

```
recite-v2/
│
├── recite-api/src/main/java/cn/bugstack/recite/api/
│   ├── IAuthService.java
│   └── dto/
│       ├── LoginRequestDTO.java
│       ├── RegisterRequestDTO.java
│       └── LoginResultDTO.java
│
├── recite-domain/src/main/java/cn/bugstack/recite/domain/auth/
│   ├── model/entity/
│   │   ├── UserEntity.java
│   │   └── AdminUserEntity.java
│   ├── port/out/
│   │   ├── UserPort.java
│   │   └── AdminUserPort.java
│   └── exception/
│       └── AuthException.java
│
├── recite-infrastructure/src/main/java/cn/bugstack/recite/infrastructure/adapter/
│   └── persistence/
│       ├── UserDO.java
│       ├── UserMapper.java
│       ├── AdminUserDO.java
│       ├── AdminUserMapper.java
│       ├── UserPersistenceAdapter.java
│       └── AdminUserPersistenceAdapter.java
│
└── recite-trigger/src/main/java/cn/bugstack/recite/trigger/
    ├── http/
    │   ├── AuthController.java
    │   └── AdminAuthController.java
    └── config/
        ├── SaTokenConfig.java
        ├── UserContext.java
        ├── UserContextInterceptor.java
        └── WebMvcConfig.java
```

**总计 20 个文件**（4 api + 6 domain + 6 infra + 4 trigger）

---

## 八、编码顺序（建议）

| 步骤 | 文件 | 原因 |
|:--:|------|------|
| 1 | `AuthException.java` | 其他类依赖 |
| 2 | `UserEntity.java` `AdminUserEntity.java` | 纯数据结构 |
| 3 | `UserPort.java` `AdminUserPort.java` | 领域契约 |
| 4 | DTO（3个）+ `IAuthService.java` | API 契约 |
| 5 | DO + Mapper（4个） | 持久层 |
| 6 | `UserPersistenceAdapter` `AdminUserPersistenceAdapter` | 实现 Port |
| 7 | `UserContext.java` `SaTokenConfig.java` | 配置依赖 |
| 8 | `UserContextInterceptor.java` `WebMvcConfig.java` | 拦截器最后 |
| 9 | `AuthController.java` `AdminAuthController.java` | 组装 |

每步完成后 `mvn compile` 验证。
