public class RepoFacadeRegistrar implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        for (String name : registry.getBeanDefinitionNames()) {
            Class<?> mapperType = resolveMapperInterface(registry.getBeanDefinition(name));
            if (mapperType == null) continue;   // MapperFactoryBean의 mapperInterface 프로퍼티에서 추출

            var def = new RootBeanDefinition(DefaultRepoFacade.class);
            def.setTargetType(ResolvableType.forClassWithGenerics(RepoFacade.class, mapperType));
            def.getConstructorArgumentValues()
            .addGenericArgumentValue(new RuntimeBeanReference(mapperBeanName));  // 매퍼는 이름으로
            def.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);       // 나머지는 타입으로 자동 주입
            registry.registerBeanDefinition(name + "Facade", def);
        }
    }
}