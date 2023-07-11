/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package v1alpha1

import (
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"k8s.io/apimachinery/pkg/util/validation/field"
	ctrl "sigs.k8s.io/controller-runtime"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/webhook"
)

// log is for logging in this package.
var backendlog = logf.Log.WithName("backend-resource")

// SetupWebhookWithManager sets up and registers the backend webhook with the manager.
func (r *Backend) SetupWebhookWithManager(mgr ctrl.Manager) error {
	return ctrl.NewWebhookManagedBy(mgr).
		For(r).
		Complete()
}

// TODO(user): EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!

//+kubebuilder:webhook:path=/mutate-dp-wso2-com-v1alpha1-backend,mutating=true,failurePolicy=fail,sideEffects=None,groups=dp.wso2.com,resources=backends,verbs=create;update,versions=v1alpha1,name=mbackend.kb.io,admissionReviewVersions=v1

var _ webhook.Defaulter = &Backend{}

// Default implements webhook.Defaulter so a webhook will be registered for the type
func (r *Backend) Default() {
	backendlog.Info("default", "name", r.Name)

	// TODO(user): fill in your defaulting logic.
}

// TODO(user): change verbs to "verbs=create;update;delete" if you want to enable deletion validation.
//+kubebuilder:webhook:path=/validate-dp-wso2-com-v1alpha1-backend,mutating=false,failurePolicy=fail,sideEffects=None,groups=dp.wso2.com,resources=backends,verbs=create;update,versions=v1alpha1,name=vbackend.kb.io,admissionReviewVersions=v1

var _ webhook.Validator = &Backend{}

// ValidateCreate implements webhook.Validator so a webhook will be registered for the type
func (r *Backend) ValidateCreate() error {
	return r.validateBackendSpec()
}

// ValidateUpdate implements webhook.Validator so a webhook will be registered for the type
func (r *Backend) ValidateUpdate(old runtime.Object) error {
	return r.validateBackendSpec()
}

// ValidateDelete implements webhook.Validator so a webhook will be registered for the type
func (r *Backend) ValidateDelete() error {
	backendlog.Info("validate delete", "name", r.Name)

	// TODO(user): fill in your validation logic upon object deletion.
	return nil
}

func (r *Backend) validateBackendSpec() error {
	var allErrs field.ErrorList
	timeout := r.Spec.Timeout
	circuitBreakers := r.Spec.CircuitBreaker
	if timeout != nil {
		if timeout.MaxRouteTimeoutSeconds < timeout.RouteTimeoutSeconds {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("timeout").Child("maxRouteTimeoutSeconds"),
				timeout.MaxRouteTimeoutSeconds, "maxRouteTimeoutSeconds should be greater than routeTimeoutSeconds"))
		}
	}
	if circuitBreakers != nil {
		if circuitBreakers.MaxConnectionPools <= 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("circuitBreaker").Child("maxConnectionPools"),
				circuitBreakers.MaxConnectionPools, "maxConnectionPools should be greater than 0"))
		}
	}
	retryConfig := r.Spec.Retry
	if retryConfig != nil {
		if int32(retryConfig.Count) < 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("retry").Child("count"), retryConfig.Count,
				"retry count should be greater than or equal to 0"))
		}
		for _, statusCode := range retryConfig.StatusCodes {
			if statusCode > 598 || statusCode < 401 {
				allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("retry").Child("statusCodes"),
					retryConfig.StatusCodes, "status code should be between 401 and 598"))
			}
		}
	}
	healthCheck := r.Spec.HealthCheck
	if healthCheck != nil {
		if healthCheck.Timeout < 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("healthCheck").Child("timeoutInMillis"),
				healthCheck.Timeout, "timeout should be greater than or equal to 0"))
		}
		if healthCheck.Interval < 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("healthCheck").Child("intervalInMillis"),
				healthCheck.Interval, "interval should be greater than or equal to 0"))
		}
		if healthCheck.UnhealthyThreshold < 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("healthCheck").Child("unhealthyThreshold"),
				healthCheck.UnhealthyThreshold, "unhealthyThreshold should be greater than or equal to 0"))
		}
		if healthCheck.HealthyThreshold < 0 {
			allErrs = append(allErrs, field.Invalid(field.NewPath("spec").Child("healthCheck").Child("healthyThreshold"),
				healthCheck.HealthyThreshold, "healthyThreshold should be greater than or equal to 0"))
		}
	}
	if len(allErrs) > 0 {
		return apierrors.NewInvalid(
			schema.GroupKind{Group: "dp.wso2.com", Kind: "Backend"},
			r.Name, allErrs)
	}
	return nil
}
