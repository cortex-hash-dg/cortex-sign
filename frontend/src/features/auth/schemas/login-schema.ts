import {z} from "zod"

export const loginSchema = z.object ({
    email: z
        .string()
        .trim()
        .min(1, 'informe seu e-mail.')
        .email('Digite seu e-mail válido.'),

    password: z.string().min(1, 'Informe sua senha.'),

    rememberMe: z.boolean(),
})

export type LoginFormData = z.infer<typeof loginSchema>