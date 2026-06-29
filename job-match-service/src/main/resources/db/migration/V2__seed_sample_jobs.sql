-- Seed 20 realistic software-engineering job listings for dev/demo.
-- Embeddings are intentionally left NULL here; they require the embedding model and are backfilled
-- at runtime by JobIngestionService (sample seeder / scheduled ingestion).
INSERT INTO job_listings
    (title, company, location, job_type, description_text, required_skills, nice_to_have_skills,
     salary_range, experience_level, source_url, external_id, active, posted_at, created_at, updated_at)
VALUES
    ('Senior Backend Engineer', 'Stripe', 'Remote', 'REMOTE',
     'Design and operate high-throughput payment APIs in Java and Go. Own services end to end, from schema design to on-call. You will work on idempotent transaction processing, ledgering, and event-driven workflows at massive scale.',
     '["Java","Spring Boot","PostgreSQL","Kafka","REST APIs","Microservices"]',
     '["Go","gRPC","Kubernetes","Distributed Systems"]',
     '$160k-$210k', 'SENIOR', 'https://stripe.com/jobs/seed-1', 'seed-1', TRUE, now(), now(), now()),

    ('Frontend Engineer', 'Vercel', 'Remote', 'REMOTE',
     'Build delightful developer-facing UIs with React and TypeScript. Care deeply about performance, accessibility, and design systems. Ship a Next.js dashboard used by millions of developers.',
     '["React","TypeScript","Next.js","CSS","HTML","Accessibility"]',
     '["TailwindCSS","GraphQL","Testing Library","Web Performance"]',
     '$140k-$180k', 'MID', 'https://vercel.com/jobs/seed-2', 'seed-2', TRUE, now(), now(), now()),

    ('Full Stack Engineer', 'Airbnb', 'San Francisco, CA', 'HYBRID',
     'Work across the stack to build booking and host tooling. Java/Kotlin on the backend, React on the frontend. Collaborate with product and design to ship features that move key metrics.',
     '["Java","React","TypeScript","MySQL","REST APIs","GraphQL"]',
     '["Kotlin","Redis","AWS","CI/CD"]',
     '$150k-$200k', 'MID', 'https://airbnb.com/jobs/seed-3', 'seed-3', TRUE, now(), now(), now()),

    ('DevOps Engineer', 'HashiCorp', 'Remote', 'REMOTE',
     'Automate infrastructure provisioning and CI/CD pipelines. Deep Terraform, Kubernetes, and cloud experience required. Champion infrastructure-as-code and reliability best practices.',
     '["Terraform","Kubernetes","AWS","Docker","CI/CD","Linux"]',
     '["Go","Prometheus","Grafana","Vault"]',
     '$150k-$195k', 'SENIOR', 'https://hashicorp.com/jobs/seed-4', 'seed-4', TRUE, now(), now(), now()),

    ('Data Engineer', 'Snowflake', 'Bellevue, WA', 'HYBRID',
     'Build batch and streaming data pipelines feeding analytics and ML. Strong SQL, Python, and Spark. Design dimensional models and own data quality.',
     '["Python","SQL","Apache Spark","Airflow","ETL","Data Modeling"]',
     '["Kafka","dbt","Snowflake","AWS"]',
     '$145k-$190k', 'MID', 'https://snowflake.com/jobs/seed-5', 'seed-5', TRUE, now(), now(), now()),

    ('Machine Learning Engineer', 'OpenAI', 'San Francisco, CA', 'ONSITE',
     'Train, evaluate, and deploy large-scale ML models in production. Python, PyTorch, and distributed training experience required. Bridge research and engineering.',
     '["Python","PyTorch","Machine Learning","Distributed Systems","Docker","Kubernetes"]',
     '["CUDA","Ray","MLOps","LLMs"]',
     '$200k-$300k', 'SENIOR', 'https://openai.com/jobs/seed-6', 'seed-6', TRUE, now(), now(), now()),

    ('Junior Software Engineer', 'Shopify', 'Remote', 'REMOTE',
     'Kickstart your career building commerce features in Ruby and React. Mentorship and a strong learning culture. You will pair with senior engineers and ship to production weekly.',
     '["Ruby","JavaScript","React","SQL","Git","REST APIs"]',
     '["Ruby on Rails","GraphQL","TypeScript"]',
     '$90k-$120k', 'JUNIOR', 'https://shopify.com/jobs/seed-7', 'seed-7', TRUE, now(), now(), now()),

    ('Site Reliability Engineer', 'Cloudflare', 'Austin, TX', 'HYBRID',
     'Keep a global edge network running at five nines. Strong Linux internals, networking, and on-call discipline. Build observability and automate toil away.',
     '["Linux","Go","Kubernetes","Prometheus","Networking","Incident Response"]',
     '["Rust","eBPF","Terraform","Grafana"]',
     '$155k-$205k', 'SENIOR', 'https://cloudflare.com/jobs/seed-8', 'seed-8', TRUE, now(), now(), now()),

    ('Principal Software Architect', 'Netflix', 'Los Gatos, CA', 'HYBRID',
     'Set technical direction for streaming platform services. Define architecture, mentor staff engineers, and drive cross-org initiatives. Deep distributed systems and JVM expertise.',
     '["Java","Distributed Systems","Microservices","Kafka","System Design","AWS"]',
     '["Cassandra","gRPC","Spinnaker","Resilience Engineering"]',
     '$250k-$400k', 'PRINCIPAL', 'https://netflix.com/jobs/seed-9', 'seed-9', TRUE, now(), now(), now()),

    ('Backend Engineer (Python)', 'Instacart', 'Remote', 'REMOTE',
     'Build fulfilment and catalog services in Python. Design APIs, optimize Postgres queries, and ship resilient services. Care about clean code and testing.',
     '["Python","Django","PostgreSQL","REST APIs","Redis","Celery"]',
     '["FastAPI","Kafka","Kubernetes","GraphQL"]',
     '$140k-$185k', 'MID', 'https://instacart.com/jobs/seed-10', 'seed-10', TRUE, now(), now(), now()),

    ('Mobile Engineer (iOS)', 'Robinhood', 'Menlo Park, CA', 'HYBRID',
     'Craft a fast, reliable trading experience in Swift. Care about animations, performance, and offline resilience. Collaborate closely with backend and design.',
     '["Swift","iOS","SwiftUI","REST APIs","Git","UIKit"]',
     '["Combine","GraphQL","Fastlane","Unit Testing"]',
     '$150k-$200k', 'MID', 'https://robinhood.com/jobs/seed-11', 'seed-11', TRUE, now(), now(), now()),

    ('Android Engineer', 'Spotify', 'New York, NY', 'HYBRID',
     'Build the Spotify Android app used by hundreds of millions. Kotlin, Jetpack Compose, and a passion for great UX. Optimize for low-end devices and flaky networks.',
     '["Kotlin","Android","Jetpack Compose","REST APIs","Git","MVVM"]',
     '["Coroutines","GraphQL","Dagger","Espresso"]',
     '$145k-$190k', 'MID', 'https://spotify.com/jobs/seed-12', 'seed-12', TRUE, now(), now(), now()),

    ('Security Engineer', 'CrowdStrike', 'Remote', 'REMOTE',
     'Harden cloud infrastructure and respond to threats. Strong AppSec, cloud security, and scripting. Build detections and drive secure-by-default patterns.',
     '["Security","AWS","Python","Linux","Threat Detection","IAM"]',
     '["Go","Kubernetes","SIEM","Terraform"]',
     '$150k-$200k', 'SENIOR', 'https://crowdstrike.com/jobs/seed-13', 'seed-13', TRUE, now(), now(), now()),

    ('Platform Engineer', 'GitLab', 'Remote', 'REMOTE',
     'Build internal developer platforms and tooling. Kubernetes, Go, and a product mindset for developer experience. Reduce friction across hundreds of engineers.',
     '["Go","Kubernetes","Docker","CI/CD","Terraform","Linux"]',
     '["Helm","ArgoCD","Prometheus","gRPC"]',
     '$145k-$190k', 'SENIOR', 'https://gitlab.com/jobs/seed-14', 'seed-14', TRUE, now(), now(), now()),

    ('Staff Backend Engineer', 'Datadog', 'New York, NY', 'HYBRID',
     'Lead design of high-cardinality metrics ingestion services. Go and distributed systems at extreme scale. Mentor engineers and own critical pipelines.',
     '["Go","Distributed Systems","Kafka","PostgreSQL","System Design","Microservices"]',
     '["Cassandra","Kubernetes","gRPC","Observability"]',
     '$190k-$260k', 'LEAD', 'https://datadoghq.com/jobs/seed-15', 'seed-15', TRUE, now(), now(), now()),

    ('Full Stack Engineer (Node)', 'Twilio', 'Remote', 'REMOTE',
     'Build messaging and voice APIs and dashboards. Node.js, TypeScript, and React. Own features end to end with strong testing habits.',
     '["Node.js","TypeScript","React","REST APIs","PostgreSQL","Git"]',
     '["GraphQL","AWS","Docker","Jest"]',
     '$135k-$180k', 'MID', 'https://twilio.com/jobs/seed-16', 'seed-16', TRUE, now(), now(), now()),

    ('Data Scientist', 'Airbnb', 'Remote', 'REMOTE',
     'Drive product decisions with rigorous experimentation and modeling. Python, SQL, and strong statistics. Partner with engineering to ship models.',
     '["Python","SQL","Statistics","Machine Learning","Pandas","Experimentation"]',
     '["Spark","A/B Testing","scikit-learn","Tableau"]',
     '$150k-$200k', 'MID', 'https://airbnb.com/jobs/seed-17', 'seed-17', TRUE, now(), now(), now()),

    ('Cloud Infrastructure Engineer', 'Coinbase', 'Remote', 'REMOTE',
     'Operate secure, scalable cloud infrastructure for crypto products. AWS, Terraform, and Kubernetes. Strong security posture and reliability focus.',
     '["AWS","Terraform","Kubernetes","Go","Linux","CI/CD"]',
     '["Vault","Prometheus","Docker","Networking"]',
     '$160k-$210k', 'SENIOR', 'https://coinbase.com/jobs/seed-18', 'seed-18', TRUE, now(), now(), now()),

    ('Engineering Manager', 'Atlassian', 'Sydney, Australia', 'HYBRID',
     'Lead a team of backend engineers building Jira platform services. Strong technical background in Java plus people-leadership skills. Drive delivery and growth.',
     '["Java","Leadership","Microservices","System Design","Agile","Mentoring"]',
     '["Spring Boot","AWS","Kafka","Coaching"]',
     '$180k-$240k', 'LEAD', 'https://atlassian.com/jobs/seed-19', 'seed-19', TRUE, now(), now(), now()),

    ('Junior Frontend Developer', 'Canva', 'Remote', 'REMOTE',
     'Join a fast-moving frontend team building design tools. React and TypeScript with mentorship from senior engineers. Strong fundamentals and eagerness to learn.',
     '["JavaScript","React","TypeScript","HTML","CSS","Git"]',
     '["Redux","Jest","Web Performance","Accessibility"]',
     '$95k-$125k', 'JUNIOR', 'https://canva.com/jobs/seed-20', 'seed-20', TRUE, now(), now(), now());
