/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.julia;

public class JuliaCodeSamples {
    protected final static String FRAGMENT_1 = "  function(Δ)\n" +
            "    x̄ = 2 .* (x * Diagonal(vec(sum(Δ; dims=2))) .- y * transpose(Δ))\n" +
            "    ȳ = 2 .* (y * Diagonal(vec(sum(Δ; dims=1))) .- x * Δ)\n" +
            "    return (nothing, f(x̄), f(ȳ))\n" +
            "  end\n" +
            "\n" +
            "@adjoint function pairwise(s::SqEuclidean, x::AbstractMatrix; dims::Int=2)\n" +
            "  if dims==1\n" +
            "    return pairwise(s, x; dims=1), ∇pairwise(s, transpose(x), transpose)\n" +
            "  else\n" +
            "    return pairwise(s, x; dims=dims), ∇pairwise(s, x, identity)\n" +
            "  end\n" +
            "end\n" +
            "\n" +
            "# comment 1\n" +
            "#= start \n" +
            "   of a multiline\n" +
            "   comment\n" +
            "=#\n" +
            "function f()\n" +
            "    howfar = 0\n" +
            "    for i in 1:10\n" +
            "        if i % 4 == 0 \n" +
            "            howfar = i \n" +
            "        end \n" +
            "    end \n" +
            "    return howfar\n" +
            "end";
    protected final static String FRAGMENT_1_CLEANED = "  function(Δ)\n" +
            "    x̄ = 2 .* (x * Diagonal(vec(sum(Δ; dims=2))) .- y * transpose(Δ))\n" +
            "    ȳ = 2 .* (y * Diagonal(vec(sum(Δ; dims=1))) .- x * Δ)\n" +
            "    return (nothing, f(x̄), f(ȳ))\n" +
            "  end\n" +
            "@adjoint function pairwise(s::SqEuclidean, x::AbstractMatrix; dims::Int=2)\n" +
            "  if dims==1\n" +
            "    return pairwise(s, x; dims=1), ∇pairwise(s, transpose(x), transpose)\n" +
            "  else\n" +
            "    return pairwise(s, x; dims=dims), ∇pairwise(s, x, identity)\n" +
            "  end\n" +
            "end\n" +
            "function f()\n" +
            "    howfar = 0\n" +
            "    for i in 1:10\n" +
            "        if i % 4 == 0 \n" +
            "            howfar = i \n" +
            "        end \n" +
            "    end \n" +
            "    return howfar\n" +
            "end";

    protected final static String FRAGMENT_1_CLEANED_FOR_DUPLICATION = "function(Δ)\n" +
            "x̄ = 2 .* (x * Diagonal(vec(sum(Δ; dims=2))) .- y * transpose(Δ))\n" +
            "ȳ = 2 .* (y * Diagonal(vec(sum(Δ; dims=1))) .- x * Δ)\n" +
            "return (nothing, f(x̄), f(ȳ))\n" +
            "end\n" +
            "@adjoint function pairwise(s::SqEuclidean, x::AbstractMatrix; dims::Int=2)\n" +
            "if dims==1\n" +
            "return pairwise(s, x; dims=1), ∇pairwise(s, transpose(x), transpose)\n" +
            "else\n" +
            "return pairwise(s, x; dims=dims), ∇pairwise(s, x, identity)\n" +
            "end\n" +
            "end\n" +
            "function f()\n" +
            "howfar = 0\n" +
            "for i in 1:10\n" +
            "if i % 4 == 0\n" +
            "howfar = i\n" +
            "end\n" +
            "end\n" +
            "return howfar\n" +
            "end";

    protected final static String BIG_FRAGMENT =
            "# This is basically a hack while we don't have a working `ldiv!`.\n" +
                    "@adjoint function \\(A::Cholesky, B::AbstractVecOrMat)\n" +
                    "  Y, back = Zygote.pullback((U, B)->U \\ (U' \\ B), A.U, B)\n" +
                    "  return Y, function(Ȳ)\n" +
                    "    Ā_factors, B̄ = back(Ȳ)\n" +
                    "    return ((uplo=nothing, status=nothing, factors=Ā_factors), B̄)\n" +
                    "  end\n" +
                    "end\n" +
                    "\n" +
                    "# Implementation due to Seeger, Matthias, et al. \"Auto-differentiating linear algebra.\"\n" +
                    "@adjoint function cholesky(Σ::Union{StridedMatrix, Symmetric{<:Real, <:StridedMatrix}})\n" +
                    "  C = cholesky(Σ)\n" +
                    "  return C, function(Δ::NamedTuple)\n" +
                    "    U, Ū = C.U, Δ.factors\n" +
                    "    Σ̄ = Ū * U'\n" +
                    "    Σ̄ = copytri!(Σ̄, 'U')\n" +
                    "    Σ̄ = ldiv!(U, Σ̄)\n" +
                    "    BLAS.trsm!('R', 'U', 'T', 'N', one(eltype(Σ)), U.data, Σ̄)\n" +
                    "    @inbounds for n in diagind(Σ̄)\n" +
                    "      Σ̄[n] /= 2\n" +
                    "    end\n" +
                    "    return (UpperTriangular(Σ̄),)\n" +
                    "  end\n" +
                    "end";
    protected final static String BIG_FRAGMENT_CLEANED =
            "@adjoint function \\(A::Cholesky, B::AbstractVecOrMat)\n" +
                    "  Y, back = Zygote.pullback((U, B)->U \\ (U' \\ B), A.U, B)\n" +
                    "  return Y, function(Ȳ)\n" +
                    "    Ā_factors, B̄ = back(Ȳ)\n" +
                    "    return ((uplo=nothing, status=nothing, factors=Ā_factors), B̄)\n" +
                    "  end\n" +
                    "end\n" +
                    "@adjoint function cholesky(Σ::Union{StridedMatrix, Symmetric{<:Real, <:StridedMatrix}})\n" +
                    "  C = cholesky(Σ)\n" +
                    "  return C, function(Δ::NamedTuple)\n" +
                    "    U, Ū = C.U, Δ.factors\n" +
                    "    Σ̄ = Ū * U'\n" +
                    "    Σ̄ = copytri!(Σ̄, 'U')\n" +
                    "    Σ̄ = ldiv!(U, Σ̄)\n" +
                    "    BLAS.trsm!('R', 'U', 'T', 'N', one(eltype(Σ)), U.data, Σ̄)\n" +
                    "    @inbounds for n in diagind(Σ̄)\n" +
                    "      Σ̄[n] /= 2\n" +
                    "    end\n" +
                    "    return (UpperTriangular(Σ̄),)\n" +
                    "  end\n" +
                    "end";
}
