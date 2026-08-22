-- Users are provisioned here rather than through a use case: decision D-13, and a non-goal in
-- CONSTITUTION.md section 6. This is demo data. The hashes are bcrypt hashes of passwords
-- printed in the README, which is exactly as insecure as it sounds and exactly what a demo
-- wants.

INSERT INTO users (id, email, name, role, password_hash) VALUES
    ('11111111-1111-4111-8111-111111111111', 'sam@pipelinecrm.demo',   'Sam Sales',  'SALES',
     '$2a$10$mK675oHzTkNTQQvmllGJXugp.ZJY6ysjOwX2sL.wfJ1EgyX6Lxwnq'),
    ('22222222-2222-4222-8222-222222222222', 'robin@pipelinecrm.demo', 'Robin Reid', 'SALES',
     '$2a$10$ud2dLTFX0HlyR1w5Dc5KEO995G37DVFsEy6hIM20HuMofHYB1jcKC'),
    ('33333333-3333-4333-8333-333333333333', 'mo@pipelinecrm.demo',    'Mo Mancini', 'MANAGER',
     '$2a$10$6mloO4X2TZpO7TTXnunL5.13dgOSGLNZJ3EFLKiXVsXucLuHMxC16');
