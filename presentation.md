page 1 (taking about 4 minutes) Introduction to System Stability
    System stability is a comprehensive topic. When we talk about stability, what we want is simply higher availability and less issues, but  achieving stability often means addressing three primary areas:
        Internal Control: Establishing a robust foundation within our system.
        External Adaptation: Allowing our system to function seamlessly, even as upstream or downstream dependencies undergo reasonable changes.
        Disaster Recovery: Implementing quick-response solutions to mitigate damage during incidents.

    To address these areas, we need solid practices in production management, observability, and contingency planning. Today, I’ll dive into how our team implements observability to support CCTS/CCDS stability.

page 2 (taking about 2 minutes) Understanding Observability in Modern Software
    To understand observability in modern software, it’s about understanding a system’s internal state through telemetry data—traces, metrics, and logs:
    Logs are timestamped records, typically used for deep analysis of specific issues, offering a “micro” view of the system.
    Capturing measurements over time, metrics give a “macro” perspective, identifying issue patterns with aggregated data like error rates or latency percentiles.
    Traces show the relationship between different components during requests, essential in distributed systems to map out interactions between services.
    A robust observability solution integrates all three to create a comprehensive view.

page 3 (taking about 4 minutes) Tailoring Observability to Our Needs
    Warming up with methodology we are following, let's see how actually we design and implement this.
    First things first, we start with requirement analysis. Besides general needs like 'few errors, better performance and faster incident response'. there are still some specific requirements for different products. For instance:
    In GLEF/casefile products, they are generating reports asynchronously, which means they do not really need low latency, but usually cost much resources. So it is important to isolate and rearrange their requests. And also sometimes users may not give us correlation id for tracing issues but only with account number. Hence we also need capability to trace by account id.
    CCI/I3/SAMS -- These systems prioritize performance, with heavy use by specific tenants like PayPal. Observability by tenant helps us identify minor tenant issues without overwhelming data from high-traffic tenants.
    After gathering requirements, we break down each into actionable observability items.
    With these broken down items, the next step would be easy.

page 4 (taking about 4 minutes) Mapping Observability in CCDS/CCTS Systems
    Our systems consist of three main components: upstream clients, internal logic, and downstream dependencies of 3 types which are 40+ PPaaS, 8+ oracle hosts, and 10+ bigquery datasets. According to items on the previous page, we map them into 3 observability telemetries: logging, tracing and metrics.
    Take internal logic as example, we need to upload internal details log. Besides trace id, the log should contain tenant and account id for searching and tracing. We also need to upload internal error count to datadog. And raptor framework already supports infra metrics. The same process for other parts.
    In essence, there are three steps: collecting requirements, breaking down solutions, and implementing. The first 2 steps (on previous page) need more efforts because we need to think in customer perspective. the last implement step would be the easiest one -- because today in PayPal we already have out-of-box instruments like datadog. 
    but besides datadog, we also use multiple other instruments to support, like CAL mainly for detailed logging, google console for bigquery specific metrics, and sometimes we also execute audit SQL manually for bigquery job details.
    even though, there are still some customized requirements need extra efforts(marked as blue). they may due to access limitation such as bigquery metrics not in datadog. or cost reason like cardinality limitation in datadog. here are 2 examples we did.

page 5 (taking about 1 minute)
    We’ve developed a dashboard with alerts for customized internal error metrics. When error rates or counts exceed thresholds, alerts are triggered, enabling us to trace issues in Datadog.

page 6 (taking about 3 minutes)
    and another example is customized CAL based metric. we upload request parameters to CAL, and use spark udf for offline analysis. we could get metrics with many dimensions such as 30 clients, 3 different error types, 4 tenants, and 720+ data points.This level of cardinality exceeds Datadog’s limits, hence the need for CAL-based metrics.

page 7 (taking about 2 minutes) Impact and Future Focus
    Since we began overseeing CCDS/CCTS a year ago, we’ve implemented internal error monitoring, fixed bugs affecting CAL logs, and corrected latency monitoring. These efforts have yielded significant benefits:
        Helping upstream clients identify long-standing bugs.
        Detecting and addressing downstream dependency issues promptly.
        Leveraging user data insights to improve design based on real usage rather than assumptions.

    While we’ve focused primarily on CCDS this past year, enhancing CCTS stability is a key goal for upcoming sprints, requiring continued observability efforts.

