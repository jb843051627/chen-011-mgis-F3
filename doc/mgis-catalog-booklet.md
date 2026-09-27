# 年度图幅目录册核收规矩 → 系统口径对照

成果管理处定下的十月收册规矩，在系统里只有一处出处：`com.fc.v2.common.submit.CatalogAccept`。
整批核收门（`/mgisCatalog/submit`）、翻账页面（按册号、按属地）、册头三数
（挂着几行、进账几行、表上一共几行）全从这一处过——回执上的数字与库里的数字
出自同一处计算，合不上便是事故，所以别处不许另写一套。

每年十月收全省十一市的年度图幅目录册。一册递进系统，逐行解析、逐行给结果：
每一行带着自己在原表里的**行次、图幅代号、成果类别、属地名址**四样；
收下的并进总账，没收下的挂着，挂着的把**行次、代号、缘故**三样齐着写。

| 处里的说法 | 系统口径 | 落点 |
| --- | --- | --- |
| 一册递进来逐行给结果 | 每一原始行都在册内行表落一条（含挂着的），行次、代号、类别、属地带着；回执分「进账」「挂着」两份明细，市里不必拿计算器一行行数 | `CatalogAccept.evaluate`、`t_mgis_catalog_line` |
| 一处填错只挂那一行 | 一行的缘故只坏它自己：挂账行进账行各算各的，坏一行绝不按住整册；挂账行缘故写全，其余行照常进账 | `CatalogAccept.evaluate`（逐行 `judgeLine`） |
| 负数幅数那一行自己挂着 | 幅数空着、不是数、≤0（负数、零）→ 该行 `HUNG`，缘故记「幅数须为正数：原值X」，带原值 | `CatalogAccept.judgeLine` |
| 四样缺一样也挂 | 代号空白、成果类别不识（不认 `QualProdType` 的四种类别）、属地空白或与封面属地不符、幅数不是正数 → 该行挂着，缘故各写各的。行次由系统按其在原表中的次序给（从 1 连排），不由市里自报，也就没有错报行次这一挂法 | `CatalogAccept.judgeLine` |
| 重递只认最早那一遍 | 同册号先查册头，头一遍已收，第二遍原样回头一遍的回执：不补一条账、挂着的也不补第二句缘故。改数走差异清单，核收记录里不许追加 | `TMgisCatalogBookletServiceImpl.submit`、`uk_catalog_booklet_no` |
| 两市同日各递一册，分两日办账一模一样 | 核收全程只看册子自己的行与经办，不看墙上的钟和别人的册子；头遍时刻由系统按落笔当口写死，重递不改它 | `TMgisCatalogBookletServiceImpl.submit` |
| 同册同代号占两行，并成一条 | 同一册内图幅代号重复：头一行 `POSTED` 建（或追加）总账，后一行 `MERGED` 指同一条总账；幅数只按一次相加（首行幅数），年报与清册口径不打架 | `CatalogAccept.evaluate`（同册去重）、`t_mgis_catalog_line.ledger_id` |
| 跨册各市各报，照常累加 | 总账按图幅代号一条（`uk_catalog_ledger_code`）；不同册的同代号是各市各报，幅数正常累加，不是重复 | `TMgisCatalogBookletServiceImpl.postLedger` |
| 行数超过上限整册不收 | 一册只许 500 行：越线当场顶回，册头、行、总账一笔都不落（一行也不落），回执都不给 | `CatalogAccept.MAX_ROWS`、`CatalogAcceptException` |
| 一张白纸不许回「已核讫」 | 零行的白表（或明细为空）当场顶回「白表不许回已核讫」，绝无收讫回执——市里不能据此以为交完了 | `CatalogAccept.evaluate` |
| 挂着加入账等于表上总行数 | 册头三数同一处算：`total_rows`=收到行数，`hanging_count`=挂着行数，`accepted_count`=**进账行数**（好行，POSTED+MERGED 都算），恒等式 `挂着 + 进账 = 总行数`；另有 `posted_count`=同册合并后总账条数。册内行结局 POSTED/MERGED/HUNG 可逐行追到 | `CatalogAccept.Evaluation`、`t_mgis_catalog_booklet` |
| 翻账两头都要翻得动 | 凭册号拉册头+逐行明细（挂着/进账分开列）；凭属地拉该地市所有册头；三数当场对得上 | `/mgisCatalog/booklet/{bookletNo}`、`/mgisCatalog/region/{region}` |
| 此门专走整批提交 | 控制器只有 submit（整本一册）与翻账查询；没有单条添、单条改的门 | `MgisCatalogController` |
| 一行行拆解入账压到服务层 | 控制器只收册子、只回回执；逐行校验、同册合并、总账追加、三数归置全在服务层与 `CatalogAccept` | `TMgisCatalogBookletServiceImpl` |
| 接口怎么回、账就怎么记 | 落库的册头/行/总账先在内存推演成定局，再一笔笔落；回执就是落库那批东西读出来的，不是另算一份 | `TMgisCatalogBookletServiceImpl.submit` |
| 名册签名一仍其旧 | `QualProdType` 四类别枚举原样复用，不改签名；要扩充另起新重载 | `CatalogAccept.judgeLine` |

## 行结局（`t_mgis_catalog_line.outcome`）

| 结局 | 含义 |
| --- | --- |
| `POSTED` | 该代号在本册头一回出现，好行，建总账或向跨册总账追加，幅数计入一次 |
| `MERGED` | 同册内代号重复的好行，并入前一条 POSTED 指的总账，**幅数不再加**；行照常留，可逐行追 |
| `HUNG` | 挂着：四样缺样、类别不识、幅数不是正数、行次重复——缘故写在 `reason`，不进总账 |

挂账缘故定句（`CatalogAccept`，只写头一遍，重递不补第二句）：

- `图幅代号未填报`
- `成果类别不识：X`
- `属地名址未填报`
- `属地名址与封面「R」不符：X`
- `幅数须为正数：X`（空、不是数、零、负数都照原样带值）

## 端点

| 端点 | 用途 |
| --- | --- |
| `POST /mgisCatalog/submit` | 整批核收（唯一写入门；权限 `mgisCatalog:submit`）。重递同册号只认头一遍 |
| `GET  /mgisCatalog/booklet/{bookletNo}` | 凭册号翻账：册头三数 + 逐行明细（进账/挂着分开） |
| `GET  /mgisCatalog/region/{region}` | 凭属地翻账：该地市各册册头，三数齐列 |
| `GET  /mgisCatalog/list` | 册头台账（按册号、属地查） |

建表脚本：`sql/mgis_catalog.sql`（册头、册内行、进账总账三表）。
