{{/*
Common labels
*/}}
{{- define "bookmyshow.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: bookmyshow
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/*
Selector labels for a service
*/}}
{{- define "bookmyshow.selectorLabels" -}}
app: {{ .name }}
app.kubernetes.io/instance: {{ .release }}
{{- end -}}

{{/*
Common environment variables
*/}}
{{- define "bookmyshow.commonEnv" -}}
- name: SPRING_PROFILES_ACTIVE
  value: "prod"
- name: DB_HOST
  value: {{ .Values.database.host | quote }}
- name: DB_PORT
  value: {{ .Values.database.port | quote }}
- name: DB_NAME
  value: {{ .Values.database.name | quote }}
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}-secrets
      key: db-username
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}-secrets
      key: db-password
- name: REDIS_HOST
  value: {{ .Values.redisConfig.host | quote }}
- name: REDIS_PORT
  value: {{ .Values.redisConfig.port | quote }}
- name: KAFKA_BOOTSTRAP_SERVERS
  value: {{ .Values.kafkaConfig.bootstrapServers | quote }}
- name: JWT_SECRET
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}-secrets
      key: jwt-secret
{{- end -}}
