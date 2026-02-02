참고 자료: https://mangkyu.tistory.com/174

https://wildeveloperetrain.tistory.com/158

디버깅

## @Valid 와 BindingResult는 어떻게 작동되는가?

우선 디스패처 서블릿에서

RequestMappingAdapter 로 선택되는 경우

여기서 여러 아규먼트리졸버들이 있음.

이중에서 아래 것만 사용한다고 가정
~~~
ServletModelAttributeMethodProcessor,
RequestResponseBodyMethodProcessor
~~~

둘다 부모를 찾아보면 비슷한 로직이 있음.
~~~
// No BindingResult yet, proceed with binding and validation
		if (bindingResult == null) {
			ResolvableType type = ResolvableType.forMethodParameter(parameter);
			WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name, type);
			if (attribute == null) {
				constructAttribute(binder, webRequest);
				attribute = wrapAsOptionalIfNecessary(parameter, binder.getTarget());
			}
			if (!binder.getBindingResult().hasErrors()) {
				if (!mavContainer.isBindingDisabled(name)) {
					bindRequestParameters(binder, webRequest);
				}
				validateIfApplicable(binder, parameter);
			}
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
			}

~~~


isBindExceptionRequired 구현은
파라미터 다음값이 있는지 그리고 Errors로 할당할 수 있는지 확인한다.

~~~
int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
~~~
이게 다 값이 맞으면 MethodArgumentNotValidException을 던짐.




binder.valdiate로 검증함.

~~~
protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				binder.validate(validationHints);
				break;
			}
		}
	}
~~~

DataBinder에서 validator는 ValidatorAdapter이며, target에 LocalValidatorFactoryBean을 가지고 있음.

LocalValidatorFactoryBean은 SpringValidatorAdapter를 상속하고있음.  또한 빈관련 인터페이스 상속



~~~
void validate(Object... validationHints) {
	validator.validate(target, bindingResult);
}

~~~

여기서는 이제 하이버네이트 구현체로 감.




## 궁금증
이러면 BindingResult가아니라 Errors의 자식들은 전부되지않을까??

가능함. Errors로 테스트해본 결과, 가능했음.
 

